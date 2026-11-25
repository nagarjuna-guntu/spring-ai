package com.rag.ingestion.config.common;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobRegistryConfig {

    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }
}
