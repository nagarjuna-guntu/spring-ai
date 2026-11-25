package com.rag.ingestion.config.common;

import org.springframework.batch.core.configuration.annotation.BatchObservabilityBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BatchObservabilityConfig {

    @Bean
    static BatchObservabilityBeanPostProcessor batchObservabilityBeanPostProcessor() {
        return new BatchObservabilityBeanPostProcessor();
    }
}
