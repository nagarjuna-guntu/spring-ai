package com.rag.ingestion.config.common;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfig {

    private final VectorStoreProperties vectorStoreProperties;

    public VectorStoreConfig(VectorStoreProperties vectorStoreProperties) {
        this.vectorStoreProperties = vectorStoreProperties;
    }

    @Bean("boardGameVectorStore")
    public VectorStore boardGameVectorStore(
            JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(vectorStoreProperties.boardGames().schemaName())
                .vectorTableName(vectorStoreProperties.boardGames().tableName())
                .indexType(vectorStoreProperties.boardGames().indexType())
                .distanceType(vectorStoreProperties.boardGames().distanceType())
                .dimensions(vectorStoreProperties.boardGames().dimensions())
                .initializeSchema(vectorStoreProperties.boardGames().initializeSchema())
                .build();
    }

    @Bean("springDocsVectorStore")
    public VectorStore springDocsVectorStore(
            JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName(vectorStoreProperties.springDocs().schemaName())
                .vectorTableName(vectorStoreProperties.springDocs().tableName())
                .indexType(vectorStoreProperties.springDocs().indexType())
                .distanceType(vectorStoreProperties.springDocs().distanceType())
                .dimensions(vectorStoreProperties.springDocs().dimensions())
                .initializeSchema(vectorStoreProperties.springDocs().initializeSchema())
                .build();
    }
}
