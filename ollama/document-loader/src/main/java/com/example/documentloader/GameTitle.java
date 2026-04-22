package com.example.documentloader;

public record GameTitle(String title) {
    public String normalizedTitle() {
        return this.title.toLowerCase().replace(" ", "_");
    }
}
