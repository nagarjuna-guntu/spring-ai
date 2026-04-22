package com.example.tools.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore
                          ) {

        return chatClientBuilder
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        vectorStoreChatMemoryAdvisor(vectorStore),
                        questionAnswerAdvisor(vectorStore)
                )
                .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().build())
                .build();
    }

    @Bean
    VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(
            VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .order(Ordered.HIGHEST_PRECEDENCE)
                .build();
    }

}
