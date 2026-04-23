package com.example.chatmemory.web;

import com.example.chatmemory.domain.ChatRequest;
import com.example.chatmemory.domain.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping
    public ChatResponse chat(@RequestHeader(name = "X_AI_CHAT_ID", defaultValue = "default") String chatId,
            @RequestBody @Valid ChatRequest chatRequest) {

        var answer = chatClient.prompt()
                .user(chatRequest.message())
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
        return new ChatResponse(answer);
    }
}
