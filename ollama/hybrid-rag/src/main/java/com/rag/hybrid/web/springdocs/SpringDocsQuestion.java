package com.rag.hybrid.web.springdocs;

import jakarta.validation.constraints.NotEmpty;

public record SpringDocsQuestion(
        @NotEmpty(message = "Question must not be empty") String question) {}
