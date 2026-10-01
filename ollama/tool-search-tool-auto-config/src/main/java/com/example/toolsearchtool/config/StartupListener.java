package com.example.toolsearchtool.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupListener {

    private static final String question = """
            Help me plan what to wear today in Visakhapatnam.
            Please suggest clothing shops that are open right now.
            """;

    private final ChatClient chatClient;

    public StartupListener(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {

        IO.println("LLM Answering Question...");

        var answer = chatClient.prompt(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-id-123"))
                .call()
                .content();

        IO.println("LLM Answer: " + answer);
    }
}
