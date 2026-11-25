package com.rag.ingestion.domain.tracking;

import com.rag.ingestion.domain.common.IngestionStatus;
import com.rag.ingestion.domain.file.FileMetadata;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Objects;

@Component
@NullMarked
public class IngestedFileLogMapper {

    public IngestedFileLog toProcessingEntity(
            FileMetadata metadata, @Nullable IngestedFileLog existing, String ingestionRequestId) {

        Assert.hasText(ingestionRequestId, "Ingestion Request ID must not be blank.");

        return baseBuilder(metadata, existing, IngestionStatus.PROCESSING)
                .ingestionRequestId(ingestionRequestId)
                .attemptCount(resolveAttemptCount(existing, ingestionRequestId))
                .lastError(null)
                .build();
    }

    public IngestedFileLog toCompletedEntity(
            FileMetadata metadata, @Nullable IngestedFileLog existing, int documentCount) {

        Assert.isTrue(documentCount >= 0, "Document count must not be negative");

        return baseBuilder(metadata, existing, IngestionStatus.COMPLETED)
                .documentCount(documentCount)
                .lastIngestedAt(Instant.now())
                .lastError(null)
                .build();
    }

    public IngestedFileLog toFailedEntity(
            FileMetadata metadata,
            @Nullable IngestedFileLog existing,
            @Nullable String errorMessage) {

        return baseBuilder(metadata, existing, IngestionStatus.FAILED)
                .lastError(errorMessage)
                .build();
    }

    private int resolveAttemptCount(@Nullable IngestedFileLog existing, String ingestionRequestId) {
        return switch (existing) {
            case null -> 1;
            case IngestedFileLog fileLog
                    when Objects.equals(fileLog.ingestionRequestId(), ingestionRequestId) ->
                    fileLog.attemptCount();
            case IngestedFileLog fileLog -> fileLog.attemptCount() + 1;
        };
    }

    private IngestedFileLog.IngestedFileLogBuilder baseBuilder(
            FileMetadata metadata, @Nullable IngestedFileLog existing, IngestionStatus status) {

        Assert.notNull(metadata, "FileMetadata must not be null.");
        Assert.notNull(status, "Ingestion status must not be null.");

        var builder =
                existing == null ? IngestedFileLog.builder().version(0) : existing.toBuilder();

        return builder.ingestionDomain(metadata.domain())
                .filePath(metadata.filePath())
                .lastModifiedTime(metadata.lastModifiedTime())
                .fileSize(metadata.fileSize())
                .ingestionStatus(status);
    }
}
