package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ETLPipelineConfig {

    @Value("classpath:/promptTemplates/game-name-prompt.st")
    Resource gameNamePromptTemplate;

    @Autowired
    FileMoverHelper fileMoverHelper;

    @Bean
    Function<Flux<Message<byte[]>>, Flux<Document>> documentReader() {
        log.info("Reading Document ...");
        return messageFlux -> messageFlux
                .publishOn(Schedulers.boundedElastic())
                .flatMap(message -> {
                    var file_originalFile = (File) message.getHeaders().get("file_originalFile");
                    assert file_originalFile != null;
                    if (!file_originalFile.exists()) {
                        log.info("file not exists...");
                        return Mono.empty();
                    }
                    String file_originalFile_path = file_originalFile.getAbsolutePath();
                    return Mono.fromCallable(() -> {
                                log.info("[{}] Reading file", file_originalFile_path);
                                var documents = new TikaDocumentReader(
                                        new ByteArrayResource(message.getPayload())
                                ).get();
                                if (documents.isEmpty()) {
                                    log.info("Empty document");
                                    throw new RuntimeException("Empty document");
                                }
                                var document = documents.getFirst();
                                document.getMetadata().put("file_originalFile_path", file_originalFile_path);
                                return document;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .timeout(Duration.ofSeconds(20))
                            .onErrorResume(ex -> {
                                log.info("[{}] Read failed -", file_originalFile_path, ex);
                                fileMoverHelper.moveToDLQ(file_originalFile);
                                return Mono.empty();
                            });
                });
    }

    @Bean
    Function<Flux<Document>, Flux<List<Document>>> documentSplitter() {
        log.info("Splitting Document ...");
        TokenTextSplitter splitter = TokenTextSplitter.builder().build();
        return documentFlux -> documentFlux
                .map(document -> splitter.apply(List.of(document)));
    }

    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient.Builder chatClientBuilder) {
        log.info("Titling Document ...");
        var chatClient = chatClientBuilder.build();
        return listFlux -> listFlux
                .onBackpressureBuffer(100)
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents ->
                        Mono.fromCallable(() -> {
                                    String combinedText = documents.stream()
                                            .limit(3)
                                            .map(Document::getText)
                                            .collect(Collectors.joining(System.lineSeparator()));
                                    log.info("processing titling---");
                                    var gameTitle = chatClient.prompt()
                                            .user(promptUserSpec -> promptUserSpec
                                                    .text(gameNamePromptTemplate)
                                                    .param("document", combinedText))
                                            .call()
                                            .entity(GameTitle.class);
                                    log.info("llm called ---{}", gameTitle);
                                    if (gameTitle == null || gameTitle.title() == null || gameTitle.title().equals("UNKNOWN")) {
                                        return Collections.<Document>emptyList();
                                    }
                                    log.info("Determined game title to be {}", gameTitle.title());
                                    documents.forEach(document ->
                                            document.getMetadata().put("gameTitle", gameTitle.normalizedTitle())
                                    );
                                    log.info("Title determined: {}", gameTitle.title());
                                    return documents;
                                }).subscribeOn(Schedulers.boundedElastic())
                                .onErrorResume(ex -> {
                                    log.error("LLM failed to find game title - ", ex);
                                    return Mono.just(Collections.emptyList());
                                }), 2
                );
    }

    @Bean
    Function<Flux<List<Document>>, Mono<Void>> documentConsumer(VectorStore vectorStore) {
        log.info("Consuming Document ...");
        return listFlux -> listFlux
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents ->
                        Mono.fromRunnable(() -> {
                                    String filePath = (String) documents.getFirst().getMetadata().get("file_originalFile_path");
                                    var count = documents.size();
                                    log.info("[{}] Writing {} documents to vector store.", filePath, count);
                                    vectorStore.accept(documents);
                                    log.info("[{}] Written {} documents to vector store.", filePath, count);
                                    fileMoverHelper.moveProcessedFile(filePath);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                ).doOnError(e -> log.error("Pipeline error - ", e))
                .then();
    }
}
