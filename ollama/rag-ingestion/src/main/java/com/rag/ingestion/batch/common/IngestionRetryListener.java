package com.rag.ingestion.batch.common;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NullMarked
public class IngestionRetryListener implements RetryListener {

    @Override
    public void beforeRetry(RetryPolicy retryPolicy, Retryable<?> retryable) {
        log.atDebug()
                .addKeyValue("operation", retryable.getName())
                .log("Retrying ingestion operation.");
    }

    @Override
    public void onRetrySuccess(
            RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Object result) {
        log.atInfo()
                .addKeyValue("operation", retryable.getName())
                .log("Ingestion operation recovered successfully after retry.");
    }

    @Override
    public void onRetryFailure(
            RetryPolicy retryPolicy, Retryable<?> retryable, @Nullable Throwable throwable) {

        String exceptionType = throwable != null ? throwable.getClass().getSimpleName() : "Unknown";
        String reason =
                throwable != null && throwable.getMessage() != null
                        ? throwable.getMessage()
                        : "No error message";
        log.atWarn()
                .addKeyValue("operation", retryable.getName())
                .addKeyValue("exceptionType", exceptionType)
                .addKeyValue("reason", reason)
                .log("Ingestion retry attempt failed.");
    }

    @Override
    public void onRetryPolicyExhaustion(
            RetryPolicy retryPolicy, Retryable<?> retryable, RetryException exception) {
        Throwable rootCause = exception.getCause();
        log.atError()
                .setCause(rootCause)
                .addKeyValue("operation", retryable.getName())
                .addKeyValue("exceptionType", rootCause.getClass().getSimpleName())
                .addKeyValue("retryCount", exception.getRetryCount())
                .log("Ingestion retry policy exhausted; no more retries available.");
    }
}
