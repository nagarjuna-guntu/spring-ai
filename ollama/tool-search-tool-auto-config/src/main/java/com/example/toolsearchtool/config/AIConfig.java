package com.example.toolsearchtool.config;

import com.example.toolsearchtool.tools.DummyTools;
import com.example.toolsearchtool.tools.MyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder
                .defaultAdvisors(MyLoggingAdvisor.builder()
                        .order(Ordered.HIGHEST_PRECEDENCE + 2000)
                        .showSystemMessage(true)
                        .showAvailableTools(true)
                        .showConversationHistory(true)
                        .build()
                );
    }

    @Bean
    ChatClientBuilderCustomizer addTools() {
        return builder -> builder
                .defaultTools(new DummyTools(), new MyTools()
                );
    }
}
