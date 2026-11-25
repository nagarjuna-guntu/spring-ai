package com.rag.ingestion.config.common;

import com.rag.ingestion.batch.gamedocs.BoardGameDocumentItemWriter;
import com.rag.ingestion.batch.lucene.LuceneItemWriter;
import com.rag.ingestion.batch.springdocs.SpringDocsItemWriter;
import com.rag.ingestion.domain.file.FileIngestionResult;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompositeWriterConfig {

    @Bean("boardGameCompositeItemWriter")
    public ItemWriter<FileIngestionResult> boardGameCompositeItemWriter(
            BoardGameDocumentItemWriter boardGameDocumentItemWriter,
            @Qualifier("boardGameLuceneItemWriter") LuceneItemWriter luceneItemWriter) {
        return new CompositeItemWriterBuilder<FileIngestionResult>()
                .delegates(boardGameDocumentItemWriter, luceneItemWriter)
                .build();
    }

    @Bean("springDocsCompositeItemWriter")
    public ItemWriter<FileIngestionResult> springDocsCompositeItemWriter(
            SpringDocsItemWriter springDocsItemWriter,
            @Qualifier("springDocsLuceneItemWriter") LuceneItemWriter luceneItemWriter) {
        return new CompositeItemWriterBuilder<FileIngestionResult>()
                .delegates(springDocsItemWriter, luceneItemWriter)
                .build();
    }
}
