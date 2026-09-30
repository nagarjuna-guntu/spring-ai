package com.example.mcpclient.config;

import com.example.mcpclient.domain.Answer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addTools(SyncMcpToolCallbackProvider toolCallbackProvider) {
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        return builder -> builder
                .defaultTools(toolCallbacks);
    }

    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder.defaultAdvisors(SimpleLoggerAdvisor.builder().build());
    }

    @Bean
    ChatClientBuilderCustomizer addValidationAdvisor() {
        var validationAdvisor = StructuredOutputValidationAdvisor
                .builder()
                .maxRepeatAttempts(2)
                .outputType(Answer.class)
                .build();
        return builder -> builder
                .defaultAdvisors(validationAdvisor);
    }

}
