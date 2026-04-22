package com.example.vectorstore_loader.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
@Slf4j
public class GameRulesLoaderPipelineFunctionConfig {

    @Value("classpath:/promptTemplates/nameOfTheGame.st")
    Resource gameNameTemplateResource;

    @Bean
    ApplicationRunner startPipeline(FunctionCatalog functionCatalog) {
        Runnable composePipelineFunction = functionCatalog.lookup(null);
        return args -> composePipelineFunction.run();
    }

    @Bean
    Function<Flux<Message<byte[]>>, Flux<Document>> documentReader() {
        return messageFlux -> messageFlux
                .flatMap(message ->
                        Mono.fromCallable(() ->
                                        new TikaDocumentReader(new ByteArrayResource(message.getPayload()))
                                                .get().getFirst())
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    @Bean
    Function<Flux<Document>, Flux<List<Document>>> splitter() {
        var splitter = TokenTextSplitter.builder().build();
        return documentFlux -> documentFlux
                .flatMap(document ->
                        Mono.fromCallable(() -> splitter.apply(List.of(document)))
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient.Builder chatChilBuilder) {

        var chatClient = chatChilBuilder.build();
        return listFlux -> listFlux
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents ->
                        // 1. Wrap the blocking LLM call in a Mono
                        Mono.fromCallable(() -> {
                                    var gameTitle = chatClient.prompt()
                                            .user(promptUserSpec -> {
                                                promptUserSpec
                                                        .text(gameNameTemplateResource)
                                                        .param("document", documents.getFirst().getText());
                                            }).call()
                                            .entity(GameTitle.class);

                                    if (Objects.requireNonNull(gameTitle).title().equals("UNKNOWN")) {
                                        log.warn("Unable to determine the name of a game; not adding to vector store.");
                                        return Collections.<Document>emptyList();
                                    }
                                    log.info("Determined game title to be {}", gameTitle.title());
                                    documents.forEach(document -> document.getMetadata().put("gameTitle", gameTitle.normalizedTitle()));
                                    return documents;
                                })
                                .subscribeOn(Schedulers.boundedElastic()) //2. Offload EACH individual LLM call to a background thread
                );
    }

    @Bean
    Consumer<Flux<List<Document>>> vectorStoreConsumer(VectorStore vectorStore) {
        return documentFlux -> documentFlux
                .filter(documents -> !documents.isEmpty())
                .doOnNext(documents -> {
                    var docCount = documents.size();
                    log.info("Writing {} documents to vector store.", docCount);
                    vectorStore.accept(documents);
                    log.info("{} documents have been written to vector store.", docCount);
                })
                .subscribe();
    }
}
