package com.rag.ingestion.domain.file;

import com.rag.ingestion.domain.common.IngestionDomain;

import java.nio.file.Path;
import java.time.Instant;

public record FileMetadata(
        IngestionDomain domain,
        String filePath,
        Instant lastModifiedTime,
        long fileSize,
        Path absolutePath) {

    public static FileMetadata of(
            IngestionDomain domain,
            String filePath,
            Instant lastModifiedTime,
            long fileSize,
            Path absolutePath) {
        return new FileMetadata(domain, filePath, lastModifiedTime, fileSize, absolutePath);
    }
}
