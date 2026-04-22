package com.example.rag_rag_advisor.domain;

import jakarta.validation.constraints.NotBlank;

public record Question(
        @NotBlank(message = "Game title is required") String gameTitle,
        @NotBlank(message = "Question is required") String question
) {
    public String normalizeTitle() {
        return this.gameTitle.toLowerCase().replace(" ", "_");
    }
}
