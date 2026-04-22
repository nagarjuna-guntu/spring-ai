package com.example.gamerulesrag.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource promptTemplate;

    private final GameRulesService gameRulesService;
    private final ChatClient chatClient;


    public BoardGameService(GameRulesService gameRulesService, ChatClient.Builder chatClientBuilder) {
        this.gameRulesService = gameRulesService;
        this.chatClient = chatClientBuilder.build();
    }

    public Answer askQuestion(Question question) {
        var gameRules = gameRulesService.findRulesFor(question.gameTitle(), question.question());
        var answer = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);

    }
}
