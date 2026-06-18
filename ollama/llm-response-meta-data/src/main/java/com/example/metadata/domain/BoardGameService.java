package com.example.metadata.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {

    @Value("classpath:/promptTemplates/systemPrompt.st")
    Resource systemPromptTemplate;

    private final ChatClient chatClient;
    private final GameRulesService gameRulesService;

    public BoardGameService(ChatClient.Builder chatClientBuilder, GameRulesService gameRulesService) {
        this.chatClient = chatClientBuilder.build();
        this.gameRulesService = gameRulesService;
    }

    public Answer askQuestion(Question question) {
        var gameRules = gameRulesService.loadGameRules(question.gameTitle());
        var responseEntity = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .responseEntity(Answer.class);
        var chatResponse = responseEntity.response();
        var metaData = chatResponse.getMetadata();
        logUsage(metaData.getUsage());
        return responseEntity.entity();
    }

    private void logUsage(Usage usage) {
        log.info("Token Usage: prompts {}, genaration {}, total {}",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }
}
