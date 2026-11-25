package com.example.sessionmemory.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.stereotype.Service;

@Service
public class AskService {

    private final ChatClient chatClient;

    public AskService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Answer ask(Question question) {
        return chatClient.prompt()
                .user(question.question())
                .advisors(SessionAdvisors.session("demo"))
                .call()
                .entity(Answer.class, spec ->
                        spec.useProviderStructuredOutput().validateSchema()
                );
    }
}
