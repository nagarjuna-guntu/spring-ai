package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;

import java.io.IOException;

@Configuration
@Slf4j
public class RedisMetadataStoreConfig {

    @Bean
    ConcurrentMetadataStore metadataStore(RedisConnectionFactory connectionFactory) {
        return new RedisMetadataStore(connectionFactory, "etl-file-metadata");
    }

    @Bean
    FileSystemPersistentAcceptOnceFileListFilter filePersistentFilter(ConcurrentMetadataStore metadataStore) {
        try(var filter =
                new FileSystemPersistentAcceptOnceFileListFilter(metadataStore, "file-filter-prefix")){
            filter.setFlushOnUpdate(true);
            return filter;
        }catch (IOException exception) {
            log.error("File filtering failed - ", exception);
            throw new RuntimeException("File filtering failed -", exception.getCause());
        }
    }
}
