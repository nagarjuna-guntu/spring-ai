package com.example.prompttemplates.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService {
    private static final String questionPromptTemplate = """
            You are a helpful assistant, answering questions about tabletop games.
            If you don't know anything about the game or don't know the answer,
            say "I don't know".
            The game is {game}.
            The question is: {question}.
            """;
    private final ChatClient chatClient;

    public BoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Answer askQuestion(Question question) {
        var answer = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(questionPromptTemplate)
                        .param("game", question.gameTitle())
                        .param("question", question.question()))
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);
    }
}
