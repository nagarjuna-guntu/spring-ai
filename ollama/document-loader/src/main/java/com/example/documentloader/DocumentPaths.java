package com.example.documentloader;

import org.springframework.boot.context.properties.ConfigurationProperties;


import java.util.Objects;

@ConfigurationProperties(prefix = "document.paths")
public record DocumentPaths(String processed, String dlq) {
    public DocumentPaths {
        processed = Objects.requireNonNullElse(processed, "D:/documents/processed");
        dlq = Objects.requireNonNullElse(dlq, "D:/documents/DLQ");
    }
}
