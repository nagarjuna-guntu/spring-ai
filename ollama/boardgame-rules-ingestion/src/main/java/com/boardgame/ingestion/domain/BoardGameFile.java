package com.boardgame.ingestion.domain;

import java.nio.file.Path;
import java.util.regex.Pattern;

public record BoardGameFile(
        Path path,
        String gameId,
        String fileName
) {
    // Pre-compile patterns once to optimize memory and performance
    private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

    public BoardGameFile {

        // Validate on construction
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (gameId == null || gameId.isBlank()) {
            throw new IllegalArgumentException("Game ID cannot be empty");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
    }

    public static BoardGameFile of(Path path, String gameId, String fileName) {
        return new BoardGameFile(path, gameId, fileName);
    }

    public String normalizedGameId() {
        //return this.title.toLowerCase().replace(" ", "_");
        // Safe to bypass null check here because the constructor prevents null titles
        String lower = this.gameId.toLowerCase();
        String cleaned = CLEAN_CHARS.matcher(lower).replaceAll("_");
        return TRIM_UNDERSCORES.matcher(cleaned).replaceAll("");
    }
}
