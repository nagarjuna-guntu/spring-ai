package com.rag.rerank.huggingface.config.lucene;

import com.rag.rerank.huggingface.domain.LuceneSearchService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LuceneSearchConfig {

    private final LuceneProperties properties;

    public LuceneSearchConfig(LuceneProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "boardGameLuceneSearch", destroyMethod = "close")
    public LuceneSearchService boardGameLuceneSearch() throws IOException {
        var normalizePath = properties.boardGames().indexPath().toAbsolutePath().normalize();
        return new LuceneSearchService(normalizePath);
    }

    @Bean(name = "springDocsLuceneSearch", destroyMethod = "close")
    public LuceneSearchService springDocsLuceneSearch() throws IOException {
        var normalizePath = properties.springDocs().indexPath().toAbsolutePath().normalize();
        return new LuceneSearchService(normalizePath);
    }
}
