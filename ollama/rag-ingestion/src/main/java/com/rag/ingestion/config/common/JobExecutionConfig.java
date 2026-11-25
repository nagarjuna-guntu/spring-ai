package com.rag.ingestion.config.common;

import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

@Configuration
public class JobExecutionConfig {

    @Bean
    TaskExecutor batchJobTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ingestion-job-");

        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(1);
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        return executor;
    }

    @Bean
    JobOperatorFactoryBean jobOperatorFactoryBean(
            JobRepository jobRepository, AsyncTaskExecutor batchJobTaskExecutor) {
        JobOperatorFactoryBean factoryBean = new JobOperatorFactoryBean();
        factoryBean.setJobRepository(jobRepository);
        factoryBean.setTaskExecutor(batchJobTaskExecutor);
        return factoryBean;
    }
}
