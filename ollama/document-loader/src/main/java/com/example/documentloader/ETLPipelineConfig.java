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
    private final ChatClient chatClient;
    private final FileMover fileMover;
    private final ConcurrentMetadataStore metadataStore;
    private final ETLPipelineProperties etlPipelineProperties;

    public ETLPipelineConfig(@Value("classpath:/promptTemplates/game-name-prompt.st") Resource gameNamePrompt, ChatClient chatClient,
                             FileMover fileMover, ConcurrentMetadataStore metadataStore, ETLPipelineProperties etlPipelineProperties) {
        this.gameNamePrompt = gameNamePrompt;
        this.chatClient = chatClient;
        this.fileMover = fileMover;
        this.metadataStore = metadataStore;
        this.etlPipelineProperties = etlPipelineProperties;
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
                .withChunkSize(etlPipelineProperties.tokenSplitter().chunkSize())
                .withMinChunkSizeChars(etlPipelineProperties.tokenSplitter().minChunkSizeChars())
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
                        .onErrorResume(ex -> handleTitleDeterminationError(getFilePath(documents), ex)), etlPipelineProperties.maxConcurrentTitles());
    }

    @Bean
    Function<Flux<List<Document>>, Mono<Void>> documentConsumer(VectorStore vectorStore) {
        return listFlux -> listFlux
                .filter(documents -> !documents.isEmpty())
                .flatMap(documents -> persistDocuments(documents, vectorStore)
                        .onErrorResume(_ -> {
                            log.error("File [{}] Consumer processing dropped an item due to error.", getFilePath(documents));
                            return Mono.empty(); // Ignore bad chunks, move forward
                        })
                ).then();
    }

    private Mono<Void> persistDocuments(List<Document> documents, VectorStore vectorStore) {
        String filePath = getFilePath(documents);
        int count = documents.size();

        return Mono.fromRunnable(() -> {
                    log.info("File [{}] with documents count {}: Vector store loading STARTED.", filePath, count);
                    vectorStore.accept(documents);
                    log.info("File [{}] with documents count {}: Vector store loading COMPLETED.", filePath, count);
                    fileMover.moveProcessedFile(filePath);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> handleVectorStoreError(filePath, ex))
                .then();
    }

    private void handleVectorStoreError(String filePath, Throwable ex) {
        log.error("File [{}] Failed to persist documents: {}", filePath, ex.getMessage(), ex);
        fileMover.moveToDLQ(filePath);
        rollbackRedisMetadata(filePath);
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
            fileMover.moveToDLQ(path);
            rollbackRedisMetadata(path);
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
            fileMover.moveToDLQ(path);
            rollbackRedisMetadata(path);
        } catch (Exception moveEx) {
            log.error("[{}] Failed to move file to DLQ: {}", path, moveEx.getMessage(), moveEx);
        }
        return Mono.empty();
    }

    private Mono<List<Document>> handleTitleDeterminationError(String path, Throwable ex) {
        log.error("[{}] LLM failed to determine game title: {}", path, ex.getMessage(), ex);
        fileMover.moveToDLQ(path);
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
                            .limit(2)
                            .map(Document::getText)
                            .collect(Collectors.joining(System.lineSeparator()));

                    log.info("Calling LLM to determine game title {}.", path);

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

                    log.info("LLM determined the game title: {}, for the path: {}", gameTitle, path);
                    documents.forEach(document ->
                            document.getMetadata().put("gameTitle", gameTitle.normalizedTitle())
                    );
                    return documents;
                })
                .subscribeOn(Schedulers.boundedElastic());
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
