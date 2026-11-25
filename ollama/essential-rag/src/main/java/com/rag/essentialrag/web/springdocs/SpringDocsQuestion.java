package com.rag.essentialrag.web.springdocs;

import jakarta.validation.constraints.NotEmpty;

public record SpringDocsQuestion(
        @NotEmpty(message = "Question must not be empty")
        String question
) {
}
