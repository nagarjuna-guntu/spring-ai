package com.rag.ingestion.config.springdocs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.ingestion.spring-docs")
public record SpringBootDocsProperties(
        Path rootDirectory, String framework, String version, int chunkSize, long skipLimit) {
    public SpringBootDocsProperties {
        Objects.requireNonNull(rootDirectory, "rootDirectory must not be null.");
        Assert.isTrue(chunkSize >= 0, "chunkSize must be a positive.");
        Assert.isTrue(skipLimit >= 0, "skipLimit must be positive.");
    }
}
