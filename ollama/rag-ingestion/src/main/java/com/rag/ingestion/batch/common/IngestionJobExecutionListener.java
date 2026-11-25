package com.rag.ingestion.batch.common;

import com.rag.ingestion.domain.tracking.IngestedFileLogTrackingManager;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class IngestionJobExecutionListener implements JobExecutionListener {

    private static final String INGESTION_REQUEST_ID = "ingestionRequestId";

    private final IngestedFileLogTrackingManager ingestedFileLogTrackingManager;

    public IngestionJobExecutionListener(
            IngestedFileLogTrackingManager ingestedFileLogTrackingManager) {
        this.ingestedFileLogTrackingManager = ingestedFileLogTrackingManager;
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {

        if (jobExecution.getStatus() != BatchStatus.FAILED) {
            return;
        }

        String jobName = jobExecution.getJobInstance().getJobName();

        switch (jobExecution.getJobParameters().getString(INGESTION_REQUEST_ID)) {
            case String ingestionRequestId when !ingestionRequestId.isBlank() ->
                    markRemainingFilesFailed(jobExecution, jobName, ingestionRequestId);
            case null, default -> logMissingParameter(jobName, jobExecution.getId());
        }
    }

    private void markRemainingFilesFailed(
            JobExecution jobExecution, String jobName, String ingestionRequestId) {

        List<Throwable> failureExceptions = jobExecution.getAllFailureExceptions();

        Throwable failure =
                switch (failureExceptions) {
                    case List<Throwable> exceptions when exceptions.isEmpty() ->
                            new IllegalStateException(
                                    "Ingestion job [%s] failed without a recorded exception.");
                    case List<Throwable> exceptions -> exceptions.getFirst();
                };

        try {

            int updatedCount =
                    ingestedFileLogTrackingManager.markProcessingFilesFailed(
                            ingestionRequestId, failure);
            log.atWarn()
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ingestionRequestId", ingestionRequestId)
                    .addKeyValue("updatedCount", updatedCount)
                    .log("Ingestion job failed; marked remaining PROCESSING files as FAILED.");

        } catch (RuntimeException e) {
            // Do not hide the original job failure if tracking cleanup also fails.
            log.atError()
                    .setCause(e)
                    .addKeyValue("jobName", jobName)
                    .addKeyValue("ingestionRequestId", ingestionRequestId)
                    .log("Failed to update remaining PROCESSING tracking records.");
        }
    }

    private void logMissingParameter(String jobName, Long jobExecutionId) {
        log.atError()
                .addKeyValue("jobName", jobName)
                .addKeyValue("jobExecutionId", jobExecutionId)
                .addKeyValue("missingParam", INGESTION_REQUEST_ID)
                .log(
                        "Cannot update ingestion tracking records: required job parameter is missing.");
    }
}
