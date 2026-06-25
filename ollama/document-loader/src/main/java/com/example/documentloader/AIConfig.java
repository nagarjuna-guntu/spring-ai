package com.example.documentloader;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {

        var validationAdvisor = StructuredOutputValidationAdvisor
                .builder()
                .maxRepeatAttempts(2)
                .outputType(GameTitle.class)
                .build();

        return chatClientBuilder
                .defaultAdvisors(validationAdvisor)
                .build();

    }
}
