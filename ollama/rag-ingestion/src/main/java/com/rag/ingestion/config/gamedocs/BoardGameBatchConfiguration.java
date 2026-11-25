package com.rag.ingestion.config.gamedocs;

import com.rag.ingestion.batch.common.IngestionJobExecutionListener;
import com.rag.ingestion.batch.common.IngestionRetryListener;
import com.rag.ingestion.batch.common.IngestionTrackingListener;
import com.rag.ingestion.batch.gamedocs.BoardGameDocumentItemProcessor;
import com.rag.ingestion.batch.gamedocs.BoardGameFile;
import com.rag.ingestion.batch.gamedocs.BoardGameFileItemReader;
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
public class BoardGameBatchConfiguration {

    private final BoardGameDocsProperties boardGameDocsProperties;

    public BoardGameBatchConfiguration(BoardGameDocsProperties boardGameDocsProperties) {
        this.boardGameDocsProperties = boardGameDocsProperties;
    }

    @Bean
    @StepScope
    public IngestionTrackingListener<BoardGameFile> boardGameFileIngestionTrackingListener(
            IngestedFileLogTrackingManager ingestedFileLogTrackingManager,
            @JobParameter("ingestionRequestId") String ingestionRequestId) {
        return new IngestionTrackingListener<>(
                ingestedFileLogTrackingManager, BoardGameFile::metadata, ingestionRequestId);
    }

    @Bean
    public Step ingestRuleBooksStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BoardGameFileItemReader boardGameFileItemReader,
            BoardGameDocumentItemProcessor boardGameDocumentItemProcessor,
            @Qualifier("boardGameCompositeItemWriter")
                    ItemWriter<FileIngestionResult> boardGameCompositeItemWriter,
            @Qualifier("ingestionRetryPolicy") RetryPolicy ingestionRetryPolicy,
            IngestionRetryListener ingestionRetryListener,
            BatchSkipPolicyFactory batchSkipPolicyFactory,
            IngestionTrackingListener<BoardGameFile> boardGameFileIngestionTrackingListener) {

        SkipPolicy skipPolicy =
                batchSkipPolicyFactory.ingestionSkipPolicy(boardGameDocsProperties.skipLimit());

        // Define your step configuration here
        return new StepBuilder("ingestRuleBooksStep", jobRepository)
                .<BoardGameFile, FileIngestionResult>chunk(boardGameDocsProperties.chunkSize())
                .transactionManager(transactionManager)
                .reader(boardGameFileItemReader)
                .processor(boardGameDocumentItemProcessor)
                .writer(boardGameCompositeItemWriter)
                .faultTolerant()
                .retryPolicy(ingestionRetryPolicy)
                .retryListener(ingestionRetryListener)
                .skipPolicy(skipPolicy)
                .listener(boardGameFileIngestionTrackingListener)
                .build();
    }

    @Bean
    public Job ingestBoardGamesJob(
            JobRepository jobRepository,
            Step ingestRuleBooksStep,
            IngestionJobExecutionListener ingestionJobExecutionListener) {
        // Define your job configuration here
        return new JobBuilder("ingestBoardGamesJob", jobRepository)
                .start(ingestRuleBooksStep)
                .listener(ingestionJobExecutionListener)
                .build();
    }
}
