package com.rag.decision.ollama.config.lucene;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.lucene")
public record LuceneProperties(IndexConfig boardGames, IndexConfig springDocs) {
    public record IndexConfig(Path indexPath) {}
}
