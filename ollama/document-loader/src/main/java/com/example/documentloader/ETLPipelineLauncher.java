package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.function.Supplier;

@Component
@Slf4j
public class ETLPipelineLauncher {


    private final FunctionCatalog functionCatalog;

    public ETLPipelineLauncher(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    void startPipeline() {
        String definition = "fileSupplier|documentReader|documentSplitter|titleDeterminer|documentConsumer";
        log.info("Launching ETL Pipeline: [{}]", definition);

        // Pattern match directly inline against the catalog wrapper lookup
        if (functionCatalog.lookup(definition) instanceof Supplier<?> supplier && supplier.get() instanceof Mono<?> pipelineMono) {
            pipelineMono
                    .doOnSubscribe(_ -> log.info(">>> ETL Engine Active - Polling File Stream <<<"))
                    .doOnError(err -> log.error(">>> Critical Pipeline Interruption: ", err))
                    // Indefinite background stream healing if infra/network disconnects
                    .retryWhen(Retry.indefinitely().filter(ex -> !(ex instanceof InterruptedException)))
                    .subscribe();
        } else {
            throw new IllegalStateException("Failed to bind reactive function layout to the catalog blueprint.");
        }
    }
}

