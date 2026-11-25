package com.rag.ingestion.domain.tracking;

import com.rag.ingestion.domain.common.IngestionStatus;
import com.rag.ingestion.domain.file.FileMetadata;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
@Slf4j
@NullMarked
public class IngestedFileLogTrackingManager {

    private static final int MAX_ERROR_LENGTH = 2000;
    private final IngestedFileLogRepository ingestedFileLogRepository;
    private final IngestedFileLogMapper ingestedFileLogMapper;

    public IngestedFileLogTrackingManager(
            IngestedFileLogRepository ingestedFileLogRepository,
            IngestedFileLogMapper ingestedFileLogMapper) {
        this.ingestedFileLogRepository = ingestedFileLogRepository;
        this.ingestedFileLogMapper = ingestedFileLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(FileMetadata metadata, String ingestionRequestId) {
        // Create or update tracking record as PROCESSING
        Assert.notNull(metadata, "File metadata cannot be null");
        Assert.notNull(ingestionRequestId, "Ingestion request ID cannot be null");

        var existing = findExistingFileLog(metadata).orElse(null);

        IngestedFileLog processingFileLog =
                ingestedFileLogMapper.toProcessingEntity(metadata, existing, ingestionRequestId);

        ingestedFileLogRepository.save(processingFileLog);

        log.atDebug()
                .addKeyValue("domain", metadata.domain())
                .addKeyValue("filePath", metadata.filePath())
                .addKeyValue("requestId", ingestionRequestId)
                .log("Marked file tracking record as PROCESSING.");
    }

    @Transactional
    public void markCompleted(FileMetadata metadata, int documentCount) {
        // Update tracking record as COMPLETED
        Assert.notNull(metadata, "File metadata cannot be null");
        Assert.isTrue(documentCount >= 0, "Document count can not be negative.");

        var existing = findExistingFileLog(metadata).orElse(null);

        IngestedFileLog completedFileLog =
                ingestedFileLogMapper.toCompletedEntity(metadata, existing, documentCount);

        ingestedFileLogRepository.save(completedFileLog);

        log.atDebug()
                .addKeyValue("domain", metadata.domain())
                .addKeyValue("filePath", metadata.filePath())
                .addKeyValue("documentCount", documentCount)
                .log("Marked file tracking record as COMPLETED.");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markProcessingFilesFailed(String ingestionRequestId, Throwable throwable) {
        // Update tracking record as FAILED
        Assert.hasText(ingestionRequestId, "Ingestion Request ID can not be null");
        Assert.notNull(throwable, "Exception can not be null");

        return ingestedFileLogRepository.markProcessingFilesFailed(
                ingestionRequestId, getErrorMessage(throwable));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedIfProcessing(FileMetadata metadata, Throwable throwable) {

        Assert.notNull(metadata, "File metadata must not be null");
        Assert.notNull(throwable, "Throwable must not be null");

        switch (findExistingFileLog(metadata).orElse(null)) {
            case IngestedFileLog existingFileLog
                    when existingFileLog.ingestionStatus() == IngestionStatus.PROCESSING -> {
                var failedFileLog =
                        ingestedFileLogMapper.toFailedEntity(
                                metadata, existingFileLog, getErrorMessage(throwable));
                ingestedFileLogRepository.save(failedFileLog);

                log.atDebug()
                        .addKeyValue("domain", metadata.domain())
                        .addKeyValue("filePath", metadata.filePath())
                        .log("Marked PROCESSING file tracking record as FAILED.");
            }
            case IngestedFileLog
                            _ -> {} // Intentionally ignore if already COMPLETED, already FAILED.
            case null -> {}
        }
    }

    private Optional<IngestedFileLog> findExistingFileLog(FileMetadata metadata) {
        return ingestedFileLogRepository.findByIngestionDomainAndFilePath(
                metadata.domain(), metadata.filePath());
    }

    private String getErrorMessage(Throwable throwable) {

        String baseException = formatErrorMessage(throwable);
        Throwable rootCause = NestedExceptionUtils.getRootCause(throwable);

        String errorSummary =
                (rootCause != null && rootCause != throwable)
                        ? String.join("| Caused by: ", baseException, formatErrorMessage(rootCause))
                        : baseException;

        return errorSummary.length() > MAX_ERROR_LENGTH
                ? errorSummary.substring(0, MAX_ERROR_LENGTH)
                : errorSummary;
    }

    private String formatErrorMessage(Throwable throwable) {
        String exceptionName = throwable.getClass().getName();
        String message = throwable.getMessage();

        return (message == null || message.isBlank())
                ? exceptionName
                : String.join(": ", exceptionName, message);
    }
}
