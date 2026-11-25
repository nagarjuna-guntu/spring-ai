package com.rag.essentialrag.config;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    @Bean("boardGameRagAdvisor")
    public Advisor boardGameRagAdvisor(
            @Qualifier("boardGameVectorStore") VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .build();
    }

    @Bean("springDocRagAdvisor")
    public Advisor springDocRagAdvisor(
            @Qualifier("springDocVectorStore") VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .build();
    }
}
