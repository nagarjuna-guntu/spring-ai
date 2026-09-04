package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ETLPipelineConfig {


    private final Resource gameNamePrompt;
    private final ChatClient chatClient;
    private final FileMover fileMover;
    private final ETLPipelineProperties etlPipelineProperties;
    private final FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter;

    public ETLPipelineConfig(@Value("classpath:/promptTemplates/game-name-prompt.st") Resource gameNamePrompt, ChatClient chatClient,
                             FileMover fileMover, ETLPipelineProperties etlPipelineProperties, FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter) {
        this.gameNamePrompt = gameNamePrompt;
        this.chatClient = chatClient;
        this.fileMover = fileMover;
        this.etlPipelineProperties = etlPipelineProperties;
        this.persistentFileFilter = persistentFileFilter;
    }

    @Bean("vectorStoreScheduler")
    Scheduler vectorStoreScheduler() {
        return Schedulers.newBoundedElastic(
                etlPipelineProperties.vectorStoreProps().batchConcurrency(), // Maximum number of concurrent threads for vector store persistence
                100, //queue size for tasks waiting to be executed
                "vector-store"
        );
    }

    @Bean
    Function<Flux<Message<byte[]>>, Flux<Document>> documentReader() {
        return messageFlux -> messageFlux
                .flatMap(message -> processDocumentRead(message)
                        .onErrorResume(ex -> {
                            var path = extractAndValidateFile(message).getAbsolutePath();
                            return handleDocumentReadError(path, ex);
                        })
                );
    }

    @Bean
    Function<Flux<Document>, Flux<List<Document>>> documentSplitter() {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(etlPipelineProperties.tokenSplitterProps().chunkSize())
                .withMinChunkSizeChars(etlPipelineProperties.tokenSplitterProps().minChunkSizeChars())
                .withKeepSeparator(true)
                .build();
        return documentFlux -> documentFlux
                .flatMap(document -> {
                    var path = getFilePath(List.of(document));
                    return Mono.fromCallable(() -> {
                                log.info("[{}] document splitting START - ", path);
                                var chunks = splitter.apply(List.of(document));
                                log.info("[{}] document splitting COMPLETED with chunks size : {}", path, chunks.size());
                                return chunks;
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .timeout(
                                    Duration.ofSeconds(20),
                                    Mono.error(new java.util.concurrent.TimeoutException("File splitting took too long!"))
                            )
                            .onErrorResume(ex -> {
                                log.error("Failed to split document chunks: {}", ex.getMessage());
                                // Handle the error and move the file to DLQ // Ignore bad chunks, move forward
                                return handleDocumentSplitterError(path, ex);
                            });
                }, 1); // Limit concurrency to 1 to avoid overwhelming the system with too many split operations
    }

    @Bean
    Function<Flux<List<Document>>, Flux<List<Document>>> titleDeterminer(ChatClient chatClient) {
        return listFlux -> listFlux
                .onBackpressureBuffer(etlPipelineProperties.backpressureBufferSize())
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents -> determineDocumentTitleByLLM(documents, chatClient)
                        .onErrorResume(ex -> {
                            var path = getFilePath(documents);
                            return handleTitleDeterminationError(path, ex);
                        })
                );
    }

    @Bean
    Function<Flux<List<Document>>, Mono<Void>> documentConsumer(VectorStore vectorStore,
                                                                @Qualifier("vectorStoreScheduler") Scheduler vectorStoreScheduler) {
        var batchSize = etlPipelineProperties.vectorStoreProps().batchSize();
        var batchConcurrency = etlPipelineProperties.vectorStoreProps().batchConcurrency();

        return listFlux -> listFlux
                .filter(documents -> !documents.isEmpty())
                .concatMap(documents ->
                        persistDocuments(documents, vectorStore, batchSize, batchConcurrency, vectorStoreScheduler)
                                .onErrorResume(ex -> {
                                    var path = getFilePath(documents);
                                    log.error("""
                                              [{}] Vector store persistence FAILED. Dropping file and moving to DLQ.
                                              Continue processing the pipeline.
                                            """, path);
                                    handleVectorStoreError(path, ex);
                                    return Mono.empty(); // Ignore bad chunks, move forward
                                })
                ).then();
    }

    private Mono<Void> persistDocuments(List<Document> documents, VectorStore vectorStore,
                                        Integer batchSize, Integer batchConcurrency, Scheduler vectorStoreScheduler) {
        String filePath = getFilePath(documents);
        int count = documents.size();

        return Mono.defer(() -> {
            log.info("[{}] Vector store persistence STARTED. chunks = {}, batch size = {}, concurrency = {}",
                    filePath, count, batchSize, batchConcurrency);
            return Flux.fromIterable(documents)
                    .buffer(batchSize)
                    .index()
                    .flatMap(indexedBatch ->
                                    persistBatch(filePath, indexedBatch.getT1() + 1,
                                            indexedBatch.getT2(), vectorStore, vectorStoreScheduler),
                            batchConcurrency)
                    .then()
                    .doOnSuccess(_ -> {
                        log.info("[{}] Vector store persistence COMPLETED. totalChunks = {}", filePath, count);
                        fileMover.moveProcessedFile(filePath);
                    });
        });
    }

    private Mono<Void> persistBatch(String filePath, long batchNumber, List<Document> batch, VectorStore vectorStore,
                                    Scheduler vectorStoreScheduler) {
        return Mono.fromRunnable(() -> {
                    log.debug("[{}] Vector store batch STARTED. batch={}, chunks={}",
                            filePath, batchNumber, batch.size());

                    vectorStore.accept(batch);

                    log.debug("[{}] Vector store batch COMPLETED. batch={}, chunks={}",
                            filePath, batchNumber, batch.size());
                })
                .subscribeOn(vectorStoreScheduler)
                .then();
    }

    private void handleVectorStoreError(String filePath, Throwable ex) {
        log.error("File [{}] Failed to persist documents: {}", filePath, ex.getMessage(), ex);
        rollbackFileAcceptance(filePath);
        fileMover.moveToDLQ(filePath);
    }

    private Mono<Document> processDocumentRead(Message<byte[]> message) {
        return Mono.defer(() -> {
            File file = extractAndValidateFile(message);
            String path = file.getAbsolutePath();
            return readDocument(message, file, path)
                    .timeout(Duration.ofSeconds(etlPipelineProperties.timeoutSeconds()));
        });
    }

    private Mono<Document> readDocument(Message<byte[]> message, File file, String path) {
        return Mono.fromCallable(() -> {
                    log.info("[{}] Reading file", path);
                    var documents = new TikaDocumentReader(new ByteArrayResource(message.getPayload())
                    ).get();
                    if (documents.isEmpty()) {
                        log.info("Extracted document contains no structured data.");
                        throw new IllegalStateException("Extracted document contains no structured data chunks.");
                    }
                    var document = documents.getFirst();
                    if (document.getText() == null || document.getText().isBlank()) {
                        log.info("Extracted document contains no text.");
                        throw new IllegalStateException("Extracted document contains no text.");
                    }
                    document.getMetadata().put("file_originalFile_path", path);
                    if (isPremiumDocument(file)) {
                        log.info("[{}] Document is identified as PREMIUM.", path);
                        document.getMetadata().put("documentType", "PREMIUM");
                    }
                    return document;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private boolean isPremiumDocument(File file) {
        var fileName = file.toPath().getFileName().toString();
        log.info("[{}] Checking if file contains premium tag.", fileName);
        int lastDotIndex = fileName.lastIndexOf('.');
        var baseFileName = lastDotIndex != -1 ? fileName.substring(0, lastDotIndex) : fileName;
        return baseFileName.endsWith("-premium");
    }

    private Mono<List<Document>> handleDocumentSplitterError(String path, Throwable ex) {
        switch (ex) {
            case java.util.concurrent.TimeoutException timeoutEx ->
                    log.error("[{}] Document read timed out: {}", path, timeoutEx.getMessage(), timeoutEx);
            case IllegalArgumentException illegalArgEx ->
                    log.error("[{}] Invalid file reference: {}", path, illegalArgEx.getMessage(), illegalArgEx);
            case Throwable throwable ->
                    log.error("[{}] Document read failed: {}", path, throwable.getMessage(), throwable);
        }
        try {
            log.info("[{}] Read Failed. Re-routing file to DLQ.", path, ex);
            rollbackFileAcceptance(path);
            fileMover.moveToDLQ(path);
        } catch (Exception moveEx) {
            log.error("[{}] Failed to move file to DLQ: {}", path, moveEx.getMessage(), moveEx);
        }
        return Mono.just(Collections.emptyList());
    }

    private Mono<Document> handleDocumentReadError(String path, Throwable ex) {
        switch (ex) {
            case java.util.concurrent.TimeoutException timeoutEx ->
                    log.error("[{}] Document read timed out: {}", path, timeoutEx.getMessage(), timeoutEx);
            case IllegalArgumentException illegalArgEx ->
                    log.error("[{}] Invalid file reference: {}", path, illegalArgEx.getMessage(), illegalArgEx);
            case IllegalStateException illegalStateEx ->
                    log.error("[{}] Document contains no text: {}", path, illegalStateEx.getMessage(), illegalStateEx);
            case Throwable throwable ->
                    log.error("[{}] Document read failed: {}", path, throwable.getMessage(), throwable);
        }
        try {
            log.info("[{}] Read Failed. Re-routing file to DLQ.", path, ex);
            rollbackFileAcceptance(path);
            fileMover.moveToDLQ(path);
        } catch (Exception moveEx) {
            log.error("[{}] Failed to move file to DLQ: {}", path, moveEx.getMessage(), moveEx);
        }
        return Mono.empty();
    }

    private Mono<List<Document>> handleTitleDeterminationError(String path, Throwable ex) {
        log.error("[{}] LLM failed to determine game title: {}", path, ex.getMessage(), ex);
        rollbackFileAcceptance(path);
        fileMover.moveToDLQ(path);
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
                            .limit(2)
                            .map(Document::getText)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining(System.lineSeparator()));

                    if (combinedText.isBlank()) {
                        throw new IllegalStateException(
                                "Document context is empty for title determination."
                        );
                    }

                    log.info("Calling LLM to determine the game title {}.", path);

                    GameTitle gameTitle = chatClient.prompt()
                            .user(promptUserSpec -> promptUserSpec
                                    .text(gameNamePrompt)
                                    .param("document", combinedText))
                            .call()
                            .entity(GameTitle.class, entityParamSpec -> entityParamSpec
                                    .useProviderStructuredOutput()
                                    .validateSchema());

                    if (gameTitle == null || !gameTitle.isValid()) {
                        log.warn("[{}] game title returned by LLM is invalid for the file - [{}].", gameTitle.title(), path);
                        throw new IllegalStateException("LLM returned an invalid game title");
                    }

                    documents.forEach(document ->
                            document.getMetadata().put("gameTitle", gameTitle.normalizedTitle())
                    );

                    log.info("[{}] Title determination COMPLETED. title=[{}]", path, gameTitle.normalizedTitle());
                    return documents;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(etlPipelineProperties.titleDeterminerProps().timeoutSeconds()));
    }

    /**
     * Rolls back file acceptance after a processing failure so the file
     * can be accepted again without restarting the application.
     */
    private void rollbackFileAcceptance(String absoluteFilePath) {
        if (absoluteFilePath == null || absoluteFilePath.isBlank()) {
            log.warn("Cannot roll back file acceptance for non-existent file: {}.", absoluteFilePath);
            return;
        }
        try {
            var filePath = Path.of(absoluteFilePath);
            if (!Files.exists(filePath)) {
                log.warn("Cannot roll back file acceptance for non-existent file: {}.", absoluteFilePath);
                return;
            }
            boolean removed = persistentFileFilter.remove(filePath.toFile());
            log.info("Rolled back file acceptance. file=[{}], removed=[{}]", absoluteFilePath, removed);
        } catch (Exception ex) {
            log.error("Failed to roll back file acceptance for file: {}", absoluteFilePath, ex);
        }
    }
}
