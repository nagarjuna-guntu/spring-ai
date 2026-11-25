package com.rag.ingestion.config.springdocs;

import com.rag.ingestion.batch.common.IngestionJobExecutionListener;
import com.rag.ingestion.batch.common.IngestionRetryListener;
import com.rag.ingestion.batch.common.IngestionTrackingListener;
import com.rag.ingestion.batch.springdocs.SpringBootDocFile;
import com.rag.ingestion.batch.springdocs.SpringBootDocsItemProcessor;
import com.rag.ingestion.batch.springdocs.SpringBootDocsItemReader;
import com.rag.ingestion.config.common.BatchSkipPolicyFactory;
import com.rag.ingestion.domain.file.FileIngestionResult;
import com.rag.ingestion.domain.tracking.IngestedFileLogTrackingManager;

import org.springframework.batch.core.configuration.annotation.JobParameter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SpringBootDocsBatchConfig {

    private final SpringBootDocsProperties springBootDocsProperties;

    public SpringBootDocsBatchConfig(SpringBootDocsProperties springBootDocsProperties) {
        this.springBootDocsProperties = springBootDocsProperties;
    }

    @Bean
    @StepScope
    public IngestionTrackingListener<SpringBootDocFile> springBootDocFileIngestionTrackingListener(
            IngestedFileLogTrackingManager ingestedFileLogTrackingManager,
            @JobParameter("ingestionRequestId") String ingestionRequestId) {
        return new IngestionTrackingListener<>(
                ingestedFileLogTrackingManager, SpringBootDocFile::metadata, ingestionRequestId);
    }

    @Bean
    public Step ingestSpringBootDocsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SpringBootDocsItemReader springBootDocsItemReader,
            SpringBootDocsItemProcessor springBootDocsItemProcessor,
            @Qualifier("springDocsCompositeItemWriter")
                    ItemWriter<FileIngestionResult> springDocsCompositeItemWriter,
            @Qualifier("ingestionRetryPolicy") RetryPolicy ingestionRetryPolicy,
            IngestionRetryListener ingestionRetryListener,
            BatchSkipPolicyFactory batchSkipPolicyFactory,
            IngestionTrackingListener<SpringBootDocFile>
                    springBootDocFileIngestionTrackingListener) {

        SkipPolicy skipPolicy =
                batchSkipPolicyFactory.ingestionSkipPolicy(springBootDocsProperties.skipLimit());

        // Define your step configuration here
        return new StepBuilder("ingestSpringBootDocsStep", jobRepository)
                .<SpringBootDocFile, FileIngestionResult>chunk(springBootDocsProperties.chunkSize())
                .transactionManager(transactionManager)
                .reader(springBootDocsItemReader)
                .processor(springBootDocsItemProcessor)
                .writer(springDocsCompositeItemWriter)
                .faultTolerant()
                .retryPolicy(ingestionRetryPolicy)
                .retryListener(ingestionRetryListener)
                .skipPolicy(skipPolicy)
                .listener(springBootDocFileIngestionTrackingListener)
                .build();
    }

    @Bean
    public Job ingestSpringBootDocsJob(
            JobRepository jobRepository,
            Step ingestSpringBootDocsStep,
            IngestionJobExecutionListener ingestionJobExecutionListener) {
        // Define your job configuration here
        return new JobBuilder("ingestSpringBootDocsJob", jobRepository)
                .start(ingestSpringBootDocsStep)
                .listener(ingestionJobExecutionListener)
                .build();
    }
}
