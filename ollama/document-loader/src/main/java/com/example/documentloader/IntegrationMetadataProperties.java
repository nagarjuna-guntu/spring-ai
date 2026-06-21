package com.example.documentloader;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "spring.integration.metadata")
public record IntegrationMetadataProperties(
        String namespace,
        String keyPrefix
) {
    public IntegrationMetadataProperties {
        namespace = Objects.requireNonNullElse(namespace, "etl-file-metadata");
        keyPrefix = Objects.requireNonNullElse(keyPrefix, "document-pipeline");
    }
}
