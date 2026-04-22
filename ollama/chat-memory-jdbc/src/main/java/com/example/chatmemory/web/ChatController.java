package com.example.chatmemory.web;

import com.example.chatmemory.domain.ChatRequest;
import com.example.chatmemory.domain.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @PostMapping
    public ChatResponse chat(@RequestBody @Valid ChatRequest chatRequest) {
        return null;
    }
}
