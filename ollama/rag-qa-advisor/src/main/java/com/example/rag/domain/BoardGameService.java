package com.example.rag.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource promptTemplate;

    private final ChatClient chatClient;

    public BoardGameService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Answer askQuestion(Question question) {
        log.info("ask Question gameTitle - {}", question.normalizeTitle());
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
                )
                .call()
                .content();
        return new Answer(question.gameTitle(), answerText);

    }
}
