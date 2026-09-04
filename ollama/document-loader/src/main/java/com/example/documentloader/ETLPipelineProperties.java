package com.example.documentloader;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import java.util.Objects;

@ConfigurationProperties(prefix = "etl.pipeline")
public record ETLPipelineProperties(
        Integer timeoutSeconds,
        Integer backpressureBufferSize,
        @Name("title-determiner")
        TitleDeterminerProps titleDeterminerProps,
        @Name("vector-store")
        VectorStoreProps vectorStoreProps,
        @Name("token-splitter")
        TokenSplitterProps tokenSplitterProps
) {
    public ETLPipelineProperties {
        // Provide safe defaults if values are omitted from configuration
        timeoutSeconds = Objects.requireNonNullElse(timeoutSeconds, 20);
        backpressureBufferSize = Objects.requireNonNullElse(backpressureBufferSize, 100);
        titleDeterminerProps = Objects.requireNonNullElse(titleDeterminerProps, new TitleDeterminerProps(2000, 2));
        vectorStoreProps = Objects.requireNonNullElse(vectorStoreProps, new VectorStoreProps(20, 2, 20));
        tokenSplitterProps = Objects.requireNonNullElse(tokenSplitterProps, new TokenSplitterProps(600, 350, 3000));
    }

    public record TitleDeterminerProps(Integer timeoutSeconds, Integer maxConcurrency) {
        public TitleDeterminerProps {
            timeoutSeconds = Objects.requireNonNullElse(timeoutSeconds, 2000);
            maxConcurrency = Objects.requireNonNullElse(maxConcurrency, 2);
        }
    }

    public record VectorStoreProps(Integer batchSize, Integer batchConcurrency, Integer timeoutSeconds) {
        public VectorStoreProps {
            batchSize = Objects.requireNonNullElse(batchSize, 20);
            batchConcurrency = Objects.requireNonNullElse(batchConcurrency, 2);
            timeoutSeconds = Objects.requireNonNullElse(timeoutSeconds, 20);
        }
    }

    public record TokenSplitterProps(Integer chunkSize, Integer minChunkSizeChars, Integer maxChunkSize) {
        public TokenSplitterProps {
            chunkSize = Objects.requireNonNullElse(chunkSize, 600);
            minChunkSizeChars = Objects.requireNonNullElse(minChunkSizeChars, 350);
            maxChunkSize = Objects.requireNonNullElse(maxChunkSize, 3000);
        }
    }
}
