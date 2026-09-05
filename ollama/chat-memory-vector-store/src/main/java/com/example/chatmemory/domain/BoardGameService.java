package com.example.chatmemory.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {


    private final Resource promptTemplate;
    private final ChatClient chatClient;

    public BoardGameService(
            @Value("classpath:/promptTemplates/systemPrompt.st") Resource promptTemplate,
            ChatClient chatClient) {
        this.promptTemplate = promptTemplate;
        this.chatClient = chatClient;
    }

    public Answer askQuestion(Question question, String chatId) {
        var gameNameMatchExpression = getDocumentFilterExpression(question);
        log.info("ask Question gameNameMatchExpression - {}", gameNameMatchExpression);

        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                )
                .user(question.question())
                .advisors(advisorSpec -> advisorSpec
                        .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, gameNameMatchExpression)
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                )
                .call()
                .entity(Answer.class, entityParamSpec -> entityParamSpec
                        .useProviderStructuredOutput()
                        .validateSchema());

    }

    private String getDocumentFilterExpression(Question question) {
        log.info("ask Question gameTitle - {}", question.normalizeTitle());
        return "gameTitle == '%s' %s".formatted(question.normalizeTitle(), getPremiumContentFilterExpression());
    }

    private String getPremiumContentFilterExpression() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isNotPremium = authorities.stream()
                .noneMatch(auth -> "ROLE_PREMIUM_USER".equals(auth.getAuthority()));
        return isNotPremium ? " AND documentType != 'PREMIUM'" : "";
    }
}
