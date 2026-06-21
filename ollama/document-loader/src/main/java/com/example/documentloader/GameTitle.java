package com.example.documentloader;

public record GameTitle(String title) {

    public GameTitle {
        // Validate on construction
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Game title cannot be empty");
        }
    }

    public boolean isValid() {
        return title != null && !title.isBlank() && !"UNKNOWN".equalsIgnoreCase(title);
    }


    public String normalizedTitle() {
        return this.title.toLowerCase().replace(" ", "_");
    }
}
