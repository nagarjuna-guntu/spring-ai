package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;

import java.io.File;

/*
  To prevent duplicate files processed twice, the file name metadata persisted in Redis Metadata store,
  verifies if the file has been already processed to prevent duplicate file processing.
  By default, Spring Integration's AcceptOnceFileListFilter tracks processed files in memory.
  If the application restarts, its memory wipes clean, causing files to be re-processed.
  The FileSystemPersistentAcceptOnceFileListFilter solves this by relying on an external persistent store
  to track state. It evaluates both the file's name and its last modified time.
  A ConcurrentMetadataStore (like RedisMetadataStore) is a thread-safe, atomic key-value store.
  Because it is hosted in Redis, multiple instances of your Spring Boot application can read and write
  to this shared data layer. If an identical file is picked up by multiple servers,
  Redis ensures only one instance successfully claims the lock to process it.
*/
@Configuration
@Slf4j
public class RedisMetadataStoreConfig {

    private final FileSupplierProperties fileSupplierProps;
    private final IntegrationMetadataProperties metadataProps;

    public RedisMetadataStoreConfig(FileSupplierProperties fileSupplierProps,
                                    IntegrationMetadataProperties metadataProps) {
        this.fileSupplierProps = fileSupplierProps;
        this.metadataProps = metadataProps;
    }

    @Bean
    ConcurrentMetadataStore metadataStore(RedisConnectionFactory connectionFactory) {
        // Name this metadata region in Redis to keep track of document states
        log.info("Initializing Redis metadata store with namespace: {}", metadataProps.namespace());
        return new RedisMetadataStore(connectionFactory, metadataProps.namespace());
    }

    @Bean
    FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter(ConcurrentMetadataStore metadataStore) {
        var persistentFileFilter = new FileSystemPersistentAcceptOnceFileListFilter(
                metadataStore,
                metadataProps.keyPrefix()
        );
        persistentFileFilter.setFlushOnUpdate(true); // Ensure updates are flushed to Redis immediately
        return persistentFileFilter;
    }

    @Bean
    ChainFileListFilter<File> fileSupplierFilter(FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter) {
        try {
            // 1. Create the regex redisFilter to maintain your (pdf|docx|txt) constraint
            RegexPatternFileListFilter regexPatternFileListFilter =
                    new RegexPatternFileListFilter(fileSupplierProps.filenameRegex());
            log.info("Configured regex filter for file extensions: {}", fileSupplierProps.filenameRegex());

            // 2. Chain them together so files must match your regex AND be unseen in Redis
            ChainFileListFilter<File> chainFileListFilter = new ChainFileListFilter<>();
            chainFileListFilter.addFilter(regexPatternFileListFilter);
            chainFileListFilter.addFilter(persistentFileFilter);
            log.info("File filter chain initialized successfully");
            return chainFileListFilter;
        } catch (Exception exception) {
            log.error("Failed to initialize file supplier filter. ", exception);
            throw new RuntimeException("Failed to initialize file supplier filter chain.", exception.getCause());
        }
    }
}
