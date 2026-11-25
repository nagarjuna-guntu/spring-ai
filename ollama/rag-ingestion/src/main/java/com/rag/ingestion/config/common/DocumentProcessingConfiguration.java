package com.rag.ingestion.config.common;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentProcessingConfiguration {

    @Bean
    TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(400) // Safely under the 512-token limit
                .withMinChunkSizeChars(200) // Allows clean paragraph breaks within smaller chunks
                .withMinChunkLengthToEmbed(50) // Filters out stray page numbers and headers
                .withKeepSeparator(true) // Preserves bullet lists and rule formatting
                .build();
    }
}
