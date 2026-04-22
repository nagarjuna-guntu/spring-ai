package com.example.boardgamebuddy.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Primary
@Slf4j
public class SpringAiBoardGameService implements BoardGameService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource promptTemplate;

    private final ChatClient chatClient;
    private final GameRulesService gameRulesService;

    public SpringAiBoardGameService(ChatClient.Builder chatClientBuilder, GameRulesService gameRulesService) {
        this.chatClient = chatClientBuilder.build();
        this.gameRulesService = gameRulesService;
    }

    @Override
    public Answer askQuestion(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
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


    public Answer askQuestionReturnsObject(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .entity(Answer.class);

    }

    public ChatResponse askQuestionReturnsResponse(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .chatResponse();
    }

    public Flux<String> askQuestionReturnsStream(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .stream()
                .content();
    }

    public Answer askQuestionLogResponseMetadata(Question question) {
        var gameRules = gameRulesService.getRulesFor(question.gameTitle());
        var responseEntity = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("rules", gameRules))
                .user(question.question())
                .call()
                .responseEntity(Answer.class);
        var response = responseEntity.response();
        assert response != null;
        ChatResponseMetadata metaData = response.getMetadata();
        logUsage(metaData.getUsage());
        return responseEntity.entity();
    }

    private void logUsage(Usage usage) {
        log.info("Token Usage prompt = {}, generation = {}, total = {}",
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());
    }

}
