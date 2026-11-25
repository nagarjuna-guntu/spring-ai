package com.example.sessionmemory.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.session.*;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.advisor.SessionTurnAdvisor;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.WindowCompactionStrategy;
import org.springframework.ai.session.jdbc.JdbcSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addSessionMemory(SessionTurnAdvisor sessionTurnAdvisor, SessionMemoryAdvisor sessionMemoryAdvisor) {
        return builder ->
                builder.defaultAdvisors(
                        sessionTurnAdvisor,
                        sessionMemoryAdvisor);
    }

    @Bean
    SessionTurnAdvisor sessionTurnAdvisor(SessionService sessionService) {
        return SessionTurnAdvisor.builder(sessionService)
                .compactionTrigger(new TurnCountTrigger(20))
                .compactionStrategy(
                        WindowCompactionStrategy.builder()
                                .lastTurns(10)
                                .build())
                .asyncCompaction(true)
                .build();
    }

    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
        return SessionMemoryAdvisor.builder(sessionService)
                .eventFilter(EventFilter.lastN(5))
                .messageFilter(MessageFilter.skipEmptyMessages())
                .build();
    }

    @Bean
    SessionRepository sessionRepository(DataSource dataSource) {
        return JdbcSessionRepository.builder()
                .dataSource(dataSource)
                .build();
    }

    @Bean
    SessionService sessionService(SessionRepository sessionRepository) {
        return DefaultSessionService.builder()
                .sessionRepository(sessionRepository)
                .build();
    }

}
