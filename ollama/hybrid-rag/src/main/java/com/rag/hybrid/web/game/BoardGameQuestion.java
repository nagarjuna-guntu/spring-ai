package com.rag.hybrid.web.game;

import jakarta.validation.constraints.NotEmpty;

import java.util.regex.Pattern;

public record BoardGameQuestion(
        @NotEmpty(message = "Game ID must not be empty") String gameId,
        @NotEmpty(message = "Question must not be empty") String question) {
    // Pre-compile patterns once to optimize memory and performance
    private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

    public String normalizeTitle() {
        // Safe to bypass null check here because the constructor prevents null titles
        String lower = this.gameId.toLowerCase();
        String cleaned = CLEAN_CHARS.matcher(lower).replaceAll("_");
        return TRIM_UNDERSCORES.matcher(cleaned).replaceAll("");
    }
}
