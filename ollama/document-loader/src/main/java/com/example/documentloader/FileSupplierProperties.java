package com.example.documentloader;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "file.supplier")
public record FileSupplierProperties(
        String directory,
        String filenameRegex,
        Integer delayWhenEmptySeconds
) {
    public FileSupplierProperties {
        directory = Objects.requireNonNullElse(directory, "D:/documents/dropoff");
        filenameRegex = Objects.requireNonNullElse(filenameRegex, ".*\\.(pdf|docx|txt)");
        delayWhenEmptySeconds = Objects.requireNonNullElse(delayWhenEmptySeconds, 5);
    }
}
