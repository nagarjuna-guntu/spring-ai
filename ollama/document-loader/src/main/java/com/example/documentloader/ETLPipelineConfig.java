package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
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


    private final Resource gameNamePrompt;
    private final FileMover fileMover;
    private final ConcurrentMetadataStore metadataStore;
    private final ETLPipelineProperties etlPipelineProperties;

    public ETLPipelineConfig(@Value("classpath:/promptTemplates/game-name-prompt.st") Resource gameNamePrompt,
                             FileMover fileMover, ConcurrentMetadataStore metadataStore, ETLPipelineProperties etlPipelineProperties) {
        this.gameNamePrompt = gameNamePrompt;
        this.fileMover = fileMover;
        this.metadataStore = metadataStore;
        this.etlPipelineProperties = etlPipelineProperties;
    }

    @Bean
    Function<Flux<Message<byte[]>>, Flux<Document>> documentReader() {
        return messageFlux -> messageFlux
                .publishOn(Schedulers.boundedElastic())
                .flatMap(message -> {
                    File file = extractAndValidateFile(message);
                    var path = file.getAbsolutePath();
                    return readDocument(message, file, path);
                });
    }

    @Bean
    Function<Flux<Document>, Flux<List<Document>>> documentSplitter() {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(etlPipelineProperties.tokenSplitter().chunkSize())
                .withMinChunkSizeChars(etlPipelineProperties.tokenSplitter().minChunkSizeChars())
                .withKeepSeparator(true)
                .build();
        return documentFlux -> documentFlux
                .map(document -> {
                    var path = getFilePath(List.of(document));
                    log.info("[{}] document splitting initialized - ", path );
                    return splitter.apply(List.of(document));
                });
    }

    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient.Builder chatClientBuilder) {
        var chatClient = chatClientBuilder.build();
        return listFlux -> listFlux
                .onBackpressureBuffer(etlPipelineProperties.backpressureBufferSize())
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents -> determineDocumentTitleByLLM(documents, chatClient), etlPipelineProperties.maxConcurrentTitles() );
    }

    @Bean
    Function<Flux<List<Document>>, Mono<Void>> documentConsumer(VectorStore vectorStore) {
        return listFlux -> listFlux
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents -> persistDocuments(documents, vectorStore))
                .then();
    }

    private Mono<Void> persistDocuments(List<Document> documents, VectorStore vectorStore) {
        String filePath = getFilePath(documents);
        int count = documents.size();

        return Mono.fromRunnable(() -> {
                    log.info("[{}] Loading {} documents to vector store", filePath, count);
                    vectorStore.accept(documents);
                    log.info("[{}] Successfully loaded {} documents to vector store", filePath, count);
                    fileMover.moveProcessedFile(filePath);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> handleVectorStoreError(filePath, ex))
                .then();
    }

    private void handleVectorStoreError(String filePath, Throwable ex) {
        log.error("[{}] Failed to persist documents: {}", filePath, ex.getMessage(), ex);
        rollbackRedisMetadata(filePath);
    }

    private Mono<Document> readDocument(Message<byte[]> message, File file, String path ) {
        return Mono.fromCallable(() -> {
                    log.info("[{}] Reading file", path);
                    var documents = new TikaDocumentReader(new ByteArrayResource(message.getPayload())
                    ).get();
                    if (documents.isEmpty()) {
                        log.info("Extracted document contains no structured data.");
                        throw new RuntimeException("Extracted document contains no structured data chunks.");
                    }
                    var document = documents.getFirst();
                    document.getMetadata().put("file_originalFile_path", path);
                    return document;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(etlPipelineProperties.timeoutSeconds()))
                .onErrorResume(ex -> handleDocumentReadError(file, path, ex));
    }

    private Mono<Document> handleDocumentReadError(File file, String path, Throwable ex) {
        log.info("[{}] Core Read Failed. Re-routing payload to DLQ.", path, ex);
        fileMover.moveToDLQ(file);
        rollbackRedisMetadata(path);
        return Mono.empty();
    }

    private Mono<List<Document>> handleTitleDeterminationError(String path, Throwable ex) {
        log.error("[{}] LLM failed to determine game title: {}", path, ex.getMessage(), ex);
        rollbackRedisMetadata(path);
        return Mono.just(Collections.emptyList());
    }
    private File extractAndValidateFile(Message<byte[]> message) {
        Object fileObj = message.getHeaders().get("file_originalFile");
        if (fileObj instanceof File file && file.exists()) {
            return file;
        }
        throw new IllegalArgumentException("Invalid file reference");
    }

    private String getFilePath(List<Document> documents) {
        return documents.stream()
                .findFirst()
                .map(doc -> doc.getMetadata().get("file_originalFile_path"))
                .map(Object::toString)
                .orElse("unknown");
    }

    private Mono<List<Document>> determineDocumentTitleByLLM(List<Document> documents, ChatClient chatClient) {
        var path = getFilePath(documents);

        return Mono.fromCallable(() -> {
                    String combinedText = documents.stream()
                            .limit(3)
                            .map(Document::getText)
                            .collect(Collectors.joining(System.lineSeparator()));

                    log.info("Calling LLM to determine game title {}.", path);

                    var gameTitle = chatClient.prompt()
                            .user(promptUserSpec -> promptUserSpec
                                    .text(gameNamePrompt)
                                    .param("document", combinedText))
                            .call()
                            .entity(GameTitle.class);

                    if (gameTitle == null || !gameTitle.isValid()) {
                        log.warn("[{}] LLM returned invalid game title.", path);
                        rollbackRedisMetadata(path);
                        return Collections.<Document>emptyList();
                    }

                    log.info("LLM determined the game title: {}, for the path: {}", gameTitle, path);
                    documents.forEach(document ->
                            document.getMetadata().put("gameTitle", gameTitle.normalizedTitle())
                    );
                    return documents;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> handleTitleDeterminationError(path, ex)
                );
    }

    /**
     * Clears the redis metadata store on failures.
     */
    private void rollbackRedisMetadata(String absoluteFilePath) {
        if (absoluteFilePath == null) {
            return;
        }

        try {
            // Spring Integration formats the key as: "prefix" + absolute_path
            String redisKey = etlPipelineProperties.redisKeyPrefix() + ":" + absoluteFilePath;
            log.info("Removing metadata key [{}] from Redis Metadata Store to allow re-processing on restart.", redisKey);
            metadataStore.remove(redisKey); // Evicts the item from Redis
        } catch (Exception ex) {
            log.error("Failed to roll back Redis metadata key for file: {}", absoluteFilePath, ex);
        }
    }
}
