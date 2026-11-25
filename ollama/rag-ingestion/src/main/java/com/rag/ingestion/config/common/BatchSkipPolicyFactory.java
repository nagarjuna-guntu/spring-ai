package com.rag.ingestion.config.common;

import com.rag.ingestion.domain.file.FileIngestionException;

import org.springframework.batch.core.step.skip.LimitCheckingExceptionHierarchySkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BatchSkipPolicyFactory {

    public SkipPolicy ingestionSkipPolicy(long skipLimit) {

        Set<Class<? extends Throwable>> skippableExceptions = Set.of(FileIngestionException.class);

        return new LimitCheckingExceptionHierarchySkipPolicy(skippableExceptions, skipLimit);
    }
}
