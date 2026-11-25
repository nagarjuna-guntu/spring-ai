package com.rag.ingestion.domain.common;

import com.rag.ingestion.web.IngestionType;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.stereotype.Component;

@Component
public class IngestionJobRegistry {

    private final JobRegistry jobRegistry;

    public IngestionJobRegistry(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    public Job getJob(IngestionType ingestionType) {
        return switch (ingestionType) {
            case BOARD_GAME_RULES -> jobRegistry.getJob("ingestBoardGamesJob");
            case SPRING_BOOT_DOCUMENTATION -> jobRegistry.getJob("ingestSpringBootDocsJob");
            case null -> throw new IllegalArgumentException("Ingestion type cannot be null");
        };
    }
}
