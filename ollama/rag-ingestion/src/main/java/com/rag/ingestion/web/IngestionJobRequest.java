package com.rag.ingestion.web;

import java.util.Map;
import java.util.Objects;

public record IngestionJobRequest(
        IngestionType ingestionType, String requestLabel, Map<String, String> parameters) {
    public IngestionJobRequest {
        Objects.requireNonNull(ingestionType, "ingestionType must not be null");
        Objects.requireNonNull(requestLabel, "requestLabel must not be null");

        parameters =
                parameters == null
                        ? Map.of()
                        : Map.copyOf(parameters); // Make a defensive copy to ensure immutability
    }
}
