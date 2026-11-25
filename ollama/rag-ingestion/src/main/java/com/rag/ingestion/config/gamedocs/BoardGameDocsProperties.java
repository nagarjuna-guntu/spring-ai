package com.rag.ingestion.config.gamedocs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.ingestion.board-game-docs")
public record BoardGameDocsProperties(Path inputDirectory, int chunkSize, long skipLimit) {
    public BoardGameDocsProperties {
        Assert.notNull(inputDirectory, "inputDirectory must not be null.");
        Assert.isTrue(chunkSize >= 0, "chunkSize must be a positive.");
        Assert.isTrue(skipLimit >= 0, "skipLimit must be positive.");
    }
}
