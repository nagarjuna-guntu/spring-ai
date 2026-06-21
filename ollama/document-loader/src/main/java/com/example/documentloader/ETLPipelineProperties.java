package com.example.documentloader;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "etl.pipeline")
public record ETLPipelineProperties(
        Integer timeoutSeconds,
        Integer maxConcurrentTitles,
        Integer backpressureBufferSize,
        String redisKeyPrefix,
        TokenSplitter tokenSplitter
) {
    public ETLPipelineProperties {
        // Provide safe defaults if values are omitted from configuration
        timeoutSeconds = Objects.requireNonNullElse(timeoutSeconds, 20);
        maxConcurrentTitles = Objects.requireNonNullElse(maxConcurrentTitles, 2);
        backpressureBufferSize = Objects.requireNonNullElse(backpressureBufferSize, 100);
        redisKeyPrefix = Objects.requireNonNullElse(redisKeyPrefix, "document-pipeline");
        tokenSplitter = Objects.requireNonNullElse(tokenSplitter, new TokenSplitter(512, 128, 3000));
    }

    public record TokenSplitter(Integer chunkSize, Integer minChunkSizeChars, Integer maxChunkSize) {
        public TokenSplitter {
            chunkSize = Objects.requireNonNullElse(chunkSize, 512);
            minChunkSizeChars = Objects.requireNonNullElse(minChunkSizeChars, 128);
            maxChunkSize = Objects.requireNonNullElse(maxChunkSize, 3000);
        }
    }
}
