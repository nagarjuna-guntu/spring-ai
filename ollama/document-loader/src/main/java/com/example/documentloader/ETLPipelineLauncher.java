package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@Slf4j
public class ETLPipelineLauncher {

    private static final String PIPELINE_DEFINITION = "fileSupplier|documentReader|documentSplitter|titleDeterminer|documentConsumer";

    private final FunctionCatalog functionCatalog;

    public ETLPipelineLauncher(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    void startPipeline() {
        log.info("Starting ETL pipeline: [{}]", PIPELINE_DEFINITION);
        var function = functionCatalog.lookup(PIPELINE_DEFINITION);

        if (!(function instanceof Supplier<?> supplier)) {
            throw new IllegalStateException("ETL pipeline [%s] could not be resolved as a Supplier"
                    .formatted(PIPELINE_DEFINITION));
        }

        Mono.defer(() -> resolvePipeline(supplier))
                .doOnSubscribe(_ -> log.info("ETL pipeline started"))
                .retryWhen(
                        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                                .maxBackoff(Duration.ofMinutes(1))
                                .doBeforeRetry(retrySignal ->
                                        log.warn("ETL pipeline failed. Restarting (attempt {})",
                                                retrySignal.totalRetries() + 1, retrySignal.failure()))
                ).subscribe();
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> resolvePipeline(Supplier<?> supplier) {
        var result = supplier.get();
        if (result instanceof Mono<?> mono) {
            return (Mono<Void>) mono;
        }
        return Mono.error(new IllegalStateException("ETL pipeline did not produce a Mono"));
    }
}

