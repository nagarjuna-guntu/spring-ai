package com.example.vectorstore_loader.config;

public record GameTitle(String title) {
    public String normalizedTitle() {
        return title.toLowerCase().replace(" ", "_");
    }
}
