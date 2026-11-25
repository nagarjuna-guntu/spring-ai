package com.boardgame.ingestion.config;

import com.boardgame.ingestion.batch.processor.BoardGameDocumentProcessor;
import com.boardgame.ingestion.batch.reader.BoardGameFileItemReader;
import com.boardgame.ingestion.batch.writer.BoardGameDocumentItemWriter;
import com.boardgame.ingestion.domain.BoardGameFile;
import org.springframework.ai.document.Document;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class BoardGameBatchConfiguration {

    @Bean
    public Step ingestRuleBooksStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    BoardGameFileItemReader boardGameFileItemReader,
                                    BoardGameDocumentProcessor boardGameDocumentProcessor,

                                    BoardGameDocumentItemWriter boardGameDocumentItemWriter) {
        // Define your step configuration here
        return new StepBuilder("ingestRuleBooksStep", jobRepository)
                .<BoardGameFile, List<Document>>chunk(1)
                .transactionManager(transactionManager)
                .reader(boardGameFileItemReader)
                .processor(boardGameDocumentProcessor)
                .writer(boardGameDocumentItemWriter)
                .build();
    }

    @Bean
    public Job ingestBoardGamesJob(JobRepository jobRepository, Step ingestRuleBooksStep) {
        // Define your job configuration here
        return new JobBuilder("ingestBoardGamesJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(ingestRuleBooksStep)
                .build();
    }
}
