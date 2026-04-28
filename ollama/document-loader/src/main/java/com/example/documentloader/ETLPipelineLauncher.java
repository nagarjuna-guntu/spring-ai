package com.example.documentloader;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Component
@Slf4j
public class ETLPipelineLauncher {


    private final FunctionCatalog functionCatalog;
    private Supplier<Mono<Void>> pipeline;

    public ETLPipelineLauncher(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    void startPipeline() {
        String functionDefinition =
                "fileSupplier|documentReader|documentSplitter|titleDeterminer|documentConsumer";
        Supplier<Mono<Void>> pipeline = functionCatalog.lookup(functionDefinition);
        Objects.requireNonNull(pipeline, "Pipeline could not be formed. Check your bean names and function definition!")
                .get()
                .doOnSubscribe(subscription -> log.info(">>> Pipeline Subscription Active - Polling Started"))
                .doOnError(e -> log.error(">>> Pipeline Stream Error: ", e))
                .subscribe();
    }
}
