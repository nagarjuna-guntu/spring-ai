package com.rag.ingestion.batch.springdocs;

import com.rag.ingestion.domain.file.FileMetadata;

import java.nio.file.Path;
import java.util.Objects;

public record SpringBootDocFile(FileMetadata metadata) {

    public SpringBootDocFile {
        Objects.requireNonNull(metadata, "File Meta data can not be null");
    }

    public static SpringBootDocFile of(FileMetadata fileMetadata) {
        return new SpringBootDocFile(fileMetadata);
    }

    public Path path() {
        return metadata.absolutePath();
    }

    public String fileName() {
        return Path.of(metadata.filePath()).getFileName().toString();
    }

    public String relativePath() {
        return metadata.filePath();
    }
}
