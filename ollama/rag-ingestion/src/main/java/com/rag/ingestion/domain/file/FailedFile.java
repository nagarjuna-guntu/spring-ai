package com.rag.ingestion.domain.file;

import java.util.Objects;

public record FailedFile(FileMetadata metadata, Exception exception) {
    public FailedFile {
        Objects.requireNonNull(metadata, "File metadata can not be null");
        Objects.requireNonNull(exception, "Exception can not be null");
    }

    public static FailedFile of(FileMetadata metadata, Exception exception) {
        return new FailedFile(metadata, exception);
    }
}
