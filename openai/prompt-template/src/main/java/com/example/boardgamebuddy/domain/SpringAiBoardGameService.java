package com.example.boardgamebuddy.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpringAiBoardGameService implements BoardGameService {
    private static final String promptTemplate = """
            You are a helpful assistant, answering questions about tabletop games.
            If you don't know anything about the game or don't know the answer,
            say "I don't know". The game is {game}. The question is: {question}.
            """;

    //@Value("classpath:/templates/promptTemplate.st")
    //Resource questionPrompt;

    private final ChatClient chatClient;

    public SpringAiBoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Answer askQuestion(Question question) {
        String prompt =
                "Answer this question about " + question.gameTitle() +
                        ": " + question.question();
        var answer = chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(promptTemplate)
                        .param("game", question.gameTitle())
                        .param("question", question.question()))
                .call()
                .content();
        return new Answer(question.gameTitle(), answer);
    }
}
