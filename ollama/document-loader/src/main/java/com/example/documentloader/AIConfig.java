package com.example.documentloader;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    /**
     * 1. The primary ChatClient bean.
     * Spring automatically applies all ChatClientBuilderCustomizer beans registered below
     * to the 'chatClientBuilder' before it arrives here.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    // 2. REGISTER A CUSTOMIZER to add advisors to the ChatClient
    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder.defaultAdvisors(SimpleLoggerAdvisor.builder().build());
    }

    // Output validation advisor for structured output
    @Bean
    ChatClientBuilderCustomizer addOutputValidation() {
        var validationAdvisor = StructuredOutputValidationAdvisor
                .builder()
                .maxRepeatAttempts(2)
                .outputType(GameTitle.class)
                .build();
        return builder -> builder.defaultAdvisors(validationAdvisor);
    }
}
