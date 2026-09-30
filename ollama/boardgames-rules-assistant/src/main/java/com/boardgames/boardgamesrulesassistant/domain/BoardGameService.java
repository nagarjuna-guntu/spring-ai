package com.boardgames.boardgamesrulesassistant.domain;

import com.boardgames.boardgamesrulesassistant.config.PromptTemplateResource;
import com.boardgames.boardgamesrulesassistant.web.Answer;
import com.boardgames.boardgamesrulesassistant.web.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@Slf4j
public class BoardGameService {

    private final ChatClient chatClient;
    private final PromptTemplateResource promptTemplateResource;

    public BoardGameService(ChatClient chatClient,
                            PromptTemplateResource promptTemplateResource) {
        this.chatClient = chatClient;
        this.promptTemplateResource = promptTemplateResource;
    }

    public Answer askQuestion(Question question, String chatId) {

        String gameTitleFilterExpression = vectorStoreFilter(question);

        log.info("askQuestion gameTitleFilterExpression: [{}]", gameTitleFilterExpression);

        Map<String, Object> advisorParams = Map.of(
                VectorStoreDocumentRetriever.FILTER_EXPRESSION, gameTitleFilterExpression,
                ChatMemory.CONVERSATION_ID, chatId);

        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplateResource.get("system"))
                        .param("gameTitle", question.gameTitle()))
                .user(question.question())
                .advisors(advisorSpec -> advisorSpec
                        .params(advisorParams))
                .call()
                .entity(Answer.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema());
    }

    private String vectorStoreFilter(Question question) {
        log.info("normalized gameTitle - [{}]", question.normalizeTitle());
        return "gameTitle == '%s' %s".formatted(question.normalizeTitle(),
                premiumContentFilterExpression());
    }

    private String premiumContentFilterExpression() {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities();
        boolean isNotPremiumUser = authorities.stream()
                .noneMatch(authority ->
                        "ROLE_PREMIUM_USER".equals(authority.getAuthority()));
        return isNotPremiumUser ? " AND documentType != 'PREMIUM'" : "";
    }
}
