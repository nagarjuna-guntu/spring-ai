package com.rag.ingestion.batch.common;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class DocumentIdGenerator {

    public UUID generate(String sourceId, int chunkIndex) {

        Assert.hasText(sourceId, "Source ID must not be null or blank");
        Assert.isTrue(chunkIndex >= 0, "Chunk index cannot be negative");

        String identity = "%s:%d".formatted(sourceId, chunkIndex);

        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }
}
