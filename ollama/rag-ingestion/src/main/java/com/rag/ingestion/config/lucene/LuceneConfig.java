package com.rag.ingestion.config.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class LuceneConfig {

    private final LuceneProperties luceneProperties;

    public LuceneConfig(LuceneProperties luceneProperties) {
        this.luceneProperties = luceneProperties;
    }

    @Bean(destroyMethod = "close")
    public Analyzer luceneAnalyzer() {
        return new StandardAnalyzer();
    }

    // --- Board Games Lucene Beans ---
    @Bean(destroyMethod = "close")
    public Directory boardGameLuceneDirectory() throws IOException {
        return createDirectory(luceneProperties.boardGames().indexPath());
    }

    @Bean(name = "boardGameLuceneIndexWriter", destroyMethod = "close")
    public IndexWriter boardGameLuceneIndexWriter(
            @Qualifier("boardGameLuceneDirectory") Directory directory, Analyzer luceneAnalyzer)
            throws IOException {
        return createIndexWriter(directory, luceneAnalyzer);
    }

    // --- Spring Docs Lucene Beans ---
    @Bean(destroyMethod = "close")
    public Directory springDocsLuceneDirectory() throws IOException {
        return createDirectory(luceneProperties.springDocs().indexPath());
    }

    @Bean(name = "springDocsLuceneIndexWriter", destroyMethod = "close")
    public IndexWriter springDocsLuceneIndexWriter(
            @Qualifier("springDocsLuceneDirectory") Directory directory, Analyzer luceneAnalyzer)
            throws IOException {
        return createIndexWriter(directory, luceneAnalyzer);
    }

    private Directory createDirectory(Path indexPath) throws IOException {
        Path normalizedPath = indexPath.toAbsolutePath().normalize();
        Files.createDirectories(normalizedPath);

        return FSDirectory.open(normalizedPath);
    }

    private IndexWriter createIndexWriter(Directory directory, Analyzer luceneAnalyzer)
            throws IOException {
        IndexWriterConfig config =
                new IndexWriterConfig(luceneAnalyzer)
                        .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(directory, config);
    }
}
