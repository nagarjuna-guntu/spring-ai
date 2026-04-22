package com.example.tools.domain;

import com.example.tools.config.FunctionToolsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {


    private final Resource promptTemplate;
    private final ChatClient chatClient;

    public BoardGameService(
            @Value("classpath:/promptTemplates/systemPromptTemplate.st")Resource promptTemplate,
            ChatClient.Builder chatClientBuilder) {
        this.promptTemplate = promptTemplate;
        this.chatClient = chatClientBuilder.build();
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
                .toolNames(FunctionToolsConfig.GAME_COMPLEXITY)
                .call()
                .content();
        return new Answer(question.gameTitle(), answerText);

    }
}
