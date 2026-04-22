package com.example.promptroles.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource systemPromptTemplate;

    private final ChatClient chatClient;
    private final GameRulesService gameRulesService;

    public BoardGameService(ChatClient.Builder chatClientBuilder, GameRulesService gameRulesService) {
        this.chatClient = chatClientBuilder.build();
        this.gameRulesService = gameRulesService;
    }

    public Answer askQuestion(Question question) {
        var gameRules = gameRulesService.loadGameRules(question.gameTitle());
        var answer = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);
    }
}
