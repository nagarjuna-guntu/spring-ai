package com.rag.ingestion.batch.gamedocs;

import com.rag.ingestion.domain.file.FileMetadata;

import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

public record BoardGameFile(FileMetadata metadata, String gameId, String fileName) {
    // Pre-compile patterns once to optimize memory and performance
    private static final Pattern CLEAN_CHARS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern TRIM_UNDERSCORES = Pattern.compile("^_+|_+$");

    public BoardGameFile {

        // Validate on construction
        Assert.notNull(metadata, "File metadata cannot be null");
        Assert.hasText(gameId, "Game ID cannot be empty");
        Assert.hasText(fileName, "File name cannot be empty");
        gameId = gameId.strip();
        fileName = fileName.strip();
    }

    public static BoardGameFile of(FileMetadata metadata, String gameId, String fileName) {
        return new BoardGameFile(metadata, gameId, fileName);
    }

    public Path path() {
        return metadata.absolutePath();
    }

    public String normalizedGameId() {
        // return this.title.toLowerCase().replace(" ", "_");
        // Safe to bypass null check here because the constructor prevents null titles
        String lower = this.gameId.toLowerCase(Locale.ROOT);
        String cleaned = CLEAN_CHARS.matcher(lower).replaceAll("_");
        return TRIM_UNDERSCORES.matcher(cleaned).replaceAll("");
    }
}
