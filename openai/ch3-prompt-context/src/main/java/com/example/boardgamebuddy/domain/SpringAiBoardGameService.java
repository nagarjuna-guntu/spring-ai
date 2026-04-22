package com.example.boardgamebuddy.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpringAiBoardGameService implements BoardGameService {

    @Value("classpath:/templates/promptTemplate.st")
    Resource questionPrompt;

    private final ChatClient chatClient;
    private final GameRulesService gameRulesService;

    public SpringAiBoardGameService(ChatClient.Builder chatClientBuilder, GameRulesService gameRulesService) {
        this.chatClient = chatClientBuilder.build();
        this.gameRulesService = gameRulesService;
    }

    @Override
    public Answer askQuestion(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
        String prompt =
                "Answer this question about " + question.gameTitle() +
                        ": " + question.question();
        var answer = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(questionPrompt)
                        .param("game", question.gameTitle())
                        .param("question", question.question())
                        .param("rules", gameRules))
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);
    }
}
