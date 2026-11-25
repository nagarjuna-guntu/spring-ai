package com.rag.ingestion.config.lucene;

import com.rag.ingestion.batch.common.DocumentMapper;
import com.rag.ingestion.batch.lucene.LuceneItemWriter;

import org.apache.lucene.index.IndexWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneItemWriterConfig {

    @Bean("boardGameLuceneItemWriter")
    public LuceneItemWriter boardGameLuceneItemWriter(
            @Qualifier("boardGameLuceneIndexWriter") IndexWriter indexWriter,
            DocumentMapper documentMapper) {
        return new LuceneItemWriter(indexWriter, documentMapper);
    }

    @Bean("springDocsLuceneItemWriter")
    public LuceneItemWriter springDocsLuceneItemWriter(
            @Qualifier("springDocsLuceneIndexWriter") IndexWriter indexWriter,
            DocumentMapper documentMapper) {
        return new LuceneItemWriter(indexWriter, documentMapper);
    }
}
