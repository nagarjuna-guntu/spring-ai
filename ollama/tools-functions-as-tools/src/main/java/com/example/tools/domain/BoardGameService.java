package com.example.tools.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {


    private final Resource promptTemplate;
    private final ChatClient chatClient;
    private final ToolCallback gameComplexityByTitle;

    public BoardGameService(
            @Value("classpath:/promptTemplates/systemPrompt.st") Resource promptTemplate,
            ChatClient chatClient, ToolCallback gameComplexityByTitle) {
        this.promptTemplate = promptTemplate;
        this.chatClient = chatClient;

        this.gameComplexityByTitle = gameComplexityByTitle;
    }

    public Answer askQuestion(Question question, String chatId) {
        log.info("ask Question gameTitle - {}", question.gameTitle());
        var gameNameMatchExpression = String.format(
                "gameTitle == '%s'", question.normalizeTitle());


        var answerText = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                )
                .user(question.question())
                .advisors(advisorSpec -> advisorSpec
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, gameNameMatchExpression)
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                )
                .tools(gameComplexityByTitle)
                .call()
                .content();
        return new Answer(question.gameTitle(), answerText);

    }
}
