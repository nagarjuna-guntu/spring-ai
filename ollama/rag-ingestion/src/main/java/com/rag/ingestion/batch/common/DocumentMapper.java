package com.rag.ingestion.batch.common;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Component
public class DocumentMapper {
    private static final String SOURCE_ID = "sourceId";
    private static final String CHUNK_INDEX = "chunkIndex";

    private final DocumentIdGenerator documentIdGenerator;

    public DocumentMapper(DocumentIdGenerator documentIdGenerator) {
        this.documentIdGenerator = documentIdGenerator;
    }

    public String sourceIdOf(Document document) {
        Assert.notNull(document, "Document cannot be null.");

        return switch (document.getMetadata().get(SOURCE_ID)) {
            case String sourceId when !sourceId.isBlank() -> sourceId;
            case null, default ->
                    throw new IllegalArgumentException(
                            "Document metadata must contain a non-blank String '%s'"
                                    .formatted(SOURCE_ID));
        };
    }

    public List<Document> toDocuments(
            List<Document> chunks, Map<String, Object> baseMetadata, String sourceId) {

        Assert.notEmpty(chunks, "Document chunks must not be empty");
        Assert.notEmpty(baseMetadata, "Base metadata must not be empty");
        Assert.hasText(sourceId, "Source ID must not be blank");

        return IntStream.range(0, chunks.size())
                .mapToObj(index -> toDocument(chunks.get(index), baseMetadata, sourceId, index))
                .toList();
    }

    private Document toDocument(
            Document chunk, Map<String, Object> baseMetadata, String sourceId, int chunkIndex) {

        Assert.notNull(chunk, "Document chunk cannot be null");

        var metadata = new HashMap<>(chunk.getMetadata());
        metadata.putAll(baseMetadata);
        metadata.put(SOURCE_ID, sourceId);
        metadata.put(CHUNK_INDEX, chunkIndex);

        var documentId = documentIdGenerator.generate(sourceId, chunkIndex);

        return Document.builder()
                .id(documentId.toString())
                .text(chunk.getText())
                .metadata(metadata)
                .build();
    }
}
