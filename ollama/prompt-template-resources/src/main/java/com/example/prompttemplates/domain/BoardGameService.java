package com.example.prompttemplates.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService {

    @Value("classpath:/promptTemplates/questionPromptTemplate.st")
    Resource questionPromptTemplate;

    private final ChatClient chatClient;

    public BoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Answer askQuestion(Question question) {
        var answer = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(questionPromptTemplate)
                        .param("gameTitle", question.gameTitle())
                        .param("question", question.question()))
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);
    }
}
