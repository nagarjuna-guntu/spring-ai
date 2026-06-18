package com.example.boardgamebuddy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameBuddyService {
    private final ChatClient chatClient;

    public BoardGameBuddyService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Answer ask(Question question) {
        log.info("ask question {}", question);
        return chatClient.prompt()
                .user(question.question())
                .call()
                .entity(Answer.class);

    }
}
