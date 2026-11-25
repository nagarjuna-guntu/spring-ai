package com.rag.rerank.huggingface.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.WindowCompactionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addSessionMemory(SessionService sessionService) {
        return builder -> {
            builder.defaultAdvisors(
                    SessionAdvisors.builder(sessionService)
                            .compaction(
                                    new TurnCountTrigger(20),
                                    WindowCompactionStrategy.builder().lastTurns(10).build())
                            .build());
        };
    }

    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder.defaultAdvisors(SimpleLoggerAdvisor.builder().build());
    }
}
