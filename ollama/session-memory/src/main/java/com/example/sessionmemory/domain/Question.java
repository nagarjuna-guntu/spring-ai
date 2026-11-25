package com.example.sessionmemory.domain;

import jakarta.validation.constraints.NotBlank;

public record Question(
        @NotBlank(message = "Question is required") String question
) {
}
