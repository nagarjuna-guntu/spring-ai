package com.example.documentloader;

import java.util.regex.Pattern;

public record GameTitle(String title) {

    // Pre-compile patterns once to optimize memory and performance
    private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

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
        //return this.title.toLowerCase().replace(" ", "_");
        // Safe to bypass null check here because the constructor prevents null titles
        String lower = this.title.toLowerCase();
        String cleaned = CLEAN_CHARS.matcher(lower).replaceAll("_");
        return TRIM_UNDERSCORES.matcher(cleaned).replaceAll("");
    }
}
