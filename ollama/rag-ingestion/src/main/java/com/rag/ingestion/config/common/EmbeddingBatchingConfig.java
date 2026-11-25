package com.rag.ingestion.config.common;

import com.knuddels.jtokkit.api.EncodingType;

import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingBatchingConfig {

    @Bean
    public BatchingStrategy tokenCountBatchingStrategy() {
        return new TokenCountBatchingStrategy(
                EncodingType.CL100K_BASE,
                5000, // max tokens per batch
                0.10 // the percentage of tokens to reserve from the max input token count to create
                // a buffer.
                );
    }
}
