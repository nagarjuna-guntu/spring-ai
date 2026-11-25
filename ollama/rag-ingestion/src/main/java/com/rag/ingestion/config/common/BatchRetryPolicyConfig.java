package com.rag.ingestion.config.common;

import org.springframework.ai.retry.TransientAiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Set;

@Configuration
public class BatchRetryPolicyConfig {

    @Bean("ingestionRetryPolicy")
    public RetryPolicy ingestionRetryPolicy() {
        return RetryPolicy.builder()
                .includes(
                        Set.of(
                                TransientAiException.class,
                                TransientDataAccessException.class,
                                ResourceAccessException.class))
                .maxRetries(2)
                .delay(Duration.ofSeconds(1))
                .multiplier(2)
                .maxDelay(Duration.ofSeconds(5))
                .jitter(Duration.ofMillis(500))
                .build();
    }
}
