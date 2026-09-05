package com.example.chatmemoryredis.domain;

import jakarta.validation.constraints.NotBlank;

import java.util.regex.Pattern;

public record Question(
        @NotBlank(message = "Game title is required") String gameTitle,
        @NotBlank(message = "Question is required") String question
) {

    // Pre-compile patterns once to optimize memory and performance
    private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

    public String normalizeTitle() {

        // Safe to bypass null check here because the constructor prevents null titles
        String lower = this.gameTitle.toLowerCase();
        String cleaned = CLEAN_CHARS.matcher(lower).replaceAll("_");
        return TRIM_UNDERSCORES.matcher(cleaned).replaceAll("");
    }
}
