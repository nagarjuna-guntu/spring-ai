package com.example.documentloader;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Component
@Slf4j
public class ETLPipelineSchedular {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final FunctionCatalog functionCatalog;
    private Supplier<Mono<Void>> pipeline;

    public ETLPipelineSchedular(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }

    @PostConstruct
    void init() {
        String functionDefinition =
                "fileSupplier|documentReader|documentSplitter|titleDeterminer|documentConsumer";
        pipeline = functionCatalog.lookup(functionDefinition);
    }

    @Scheduled(fixedDelay = 20000) //15S as uses LLM call to decide the title
    void scheduleEtlPipeline() {
        if (!running.compareAndSet(false, true)) {
            log.info("Pipeline already running, skipping...");
            return;
        }
            log.info("Starting ETL pipeline...");
            pipeline.get()
                    .doFinally(signal -> {
                        running.set(false);
                        log.info("Pipeline finished with signal: {}", signal);
                    })
                    .subscribe();
        }
}
