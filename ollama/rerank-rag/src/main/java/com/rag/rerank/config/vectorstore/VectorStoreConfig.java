package com.rag.rerank.config.vectorstore;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    private final VectorStoreProperties properties;

    public VectorStoreConfig(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Bean("boardGameVectorStore")
    public VectorStore boardGameVectorStore(
            JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(properties.boardGames().schemaName())
                .vectorTableName(properties.boardGames().tableName())
                .initializeSchema(properties.boardGames().initializeSchema())
                .build();
    }

    @Bean("springDocsVectorStore")
    public VectorStore springDocsVectorStore(
            JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(properties.springDocs().schemaName())
                .vectorTableName(properties.springDocs().tableName())
                .initializeSchema(properties.springDocs().initializeSchema())
                .build();
    }
}
