package com.rag.ingestion.web;

public record IngestionJobResponse(
        String jobName,
        Long executionId,
        String status,
        String requestLabel,
        String jobIngestionRequestId) {
    public static IngestionJobResponse of(
            String jobName,
            Long executionId,
            String status,
            String requestLabel,
            String jobIngestionRequestId) {
        return new IngestionJobResponse(
                jobName, executionId, status, requestLabel, jobIngestionRequestId);
    }
}
