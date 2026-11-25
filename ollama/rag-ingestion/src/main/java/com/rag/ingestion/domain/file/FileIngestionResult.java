package com.rag.ingestion.domain.file;

import org.springframework.ai.document.Document;
import org.springframework.util.Assert;

import java.util.List;

public record FileIngestionResult(FileMetadata metadata, List<Document> documents) {
    public FileIngestionResult {
        Assert.notNull(metadata, "File metadata cannot be null");
        documents = List.copyOf(documents);
    }

    public static FileIngestionResult of(FileMetadata metadata, List<Document> documents) {
        return new FileIngestionResult(metadata, documents);
    }

    public int documentCont() {
        return documents.size();
    }
}
