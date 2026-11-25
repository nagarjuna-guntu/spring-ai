package com.rag.hybrid.config.vectorstore;

import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vectorstore")
public record VectorStoreProperties(PgVectorConfig boardGames, PgVectorConfig springDocs) {
    public record PgVectorConfig(
            String schemaName,
            String tableName,
            PgVectorStore.PgIndexType indexType,
            PgVectorStore.PgDistanceType distanceType,
            int dimensions,
            boolean initializeSchema) {}
}
