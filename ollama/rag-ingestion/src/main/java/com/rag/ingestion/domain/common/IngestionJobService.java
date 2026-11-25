package com.rag.ingestion.domain.common;

import com.rag.ingestion.web.IngestionJobRequest;
import com.rag.ingestion.web.IngestionJobResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class IngestionJobService {

    private static final DateTimeFormatter REQUEST_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private static final String INGESTION_REQUEST_ID = "ingestionRequestId";

    private final JobOperator jobOperator;
    private final IngestionJobRegistry jobRegistry;

    public IngestionJobService(JobOperator jobOperator, IngestionJobRegistry jobRegistry) {
        this.jobOperator = jobOperator;
        this.jobRegistry = jobRegistry;
    }

    public IngestionJobResponse start(IngestionJobRequest jobRequest) throws Exception {

        Job job = jobRegistry.getJob(jobRequest.ingestionType());

        var jobParametersBuilder = new JobParametersBuilder();

        jobRequest
                .parameters()
                .forEach((key, value) -> jobParametersBuilder.addString(key, value, true));

        String ingestionRequestId =
                generateJobIngestionRequestId(job.getName(), jobRequest.requestLabel());

        jobParametersBuilder.addString(INGESTION_REQUEST_ID, ingestionRequestId, true);

        JobParameters jobParameters = jobParametersBuilder.toJobParameters();

        jobParameters
                .getIdentifyingParameters()
                .forEach(parameter -> log.info("Job parameter: {} ", parameter));

        var jobExecution = jobOperator.start(job, jobParameters);
        var jobIngestionRequestId =
                jobExecution
                        .getJobParameters()
                        .getString(INGESTION_REQUEST_ID, "Job Ingestion Request ID Not Available.");
        return IngestionJobResponse.of(
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                jobExecution.getStatus().toString(),
                jobRequest.requestLabel(),
                jobIngestionRequestId);
    }

    private String generateJobIngestionRequestId(String jobName, String requestLabel) {
        return "%s-%s-%s-%s"
                .formatted(
                        jobName,
                        requestLabel,
                        REQUEST_ID_FORMAT.format(Instant.now()),
                        UUID.randomUUID());
    }
}
