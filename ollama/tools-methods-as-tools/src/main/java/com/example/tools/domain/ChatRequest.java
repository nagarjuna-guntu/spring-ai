package com.example.tools.domain;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "Message cannot be empty") String message,
        @NotBlank(message = "Chat ID is required for history tracking") String chatId
) {
}
