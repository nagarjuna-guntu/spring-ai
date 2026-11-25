package com.rag.hybrid.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.Assert;

import java.util.Objects;

@Slf4j
public class SpringDocHydeQueryTransformer implements QueryTransformer {

    private static final String DEFAULT_PROMPT =
            """
            Write a short passage that would appear in official Spring Boot technical
            documentation to answer the following question.

            Do not mention that the passage is hypothetical.
            Do not explain your reasoning.
            Do not ask follow-up questions.
            Use accurate Spring Boot, Java, and configuration terminology that a
            reference guide would use.

            Question:
            {query}

            Documentation passage:
            """;

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;

    public SpringDocHydeQueryTransformer(
            ChatClient.Builder chatClientBuilder, PromptTemplate promptTemplate) {
        Objects.requireNonNull(chatClientBuilder, "ChatClientBuilder can not be null.");
        Objects.requireNonNull(promptTemplate, "PromptTemplate can not be null.");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NonNull Query transform(@NonNull Query query) {
        Assert.notNull(query, "query must not be null.");
        Assert.hasText(query.text(), "query text must not be empty.");

        log.atDebug()
                .addKeyValue("documentType", "Spring_Boot_Docs")
                .addKeyValue("originalQuery", query.text())
                .log("Transforming query with HyDE.");

        String hypotheticalDocument =
                this.chatClient
                        .prompt()
                        .user(user ->
                                user.text(this.promptTemplate.getTemplate())
                                        .param("query", query.text()))
                        .call()
                        .content();
        Assert.hasText(hypotheticalDocument, "The model did not generate hypothetical document.");
        String strippedExcerpt = hypotheticalDocument.strip();

        log.atDebug()
                .addKeyValue("documentType", "Spring_Boot_Docs")
                .addKeyValue("hydeExcerpt", strippedExcerpt)
                .log("Generated HyDE excerpt.");

        return query.mutate().text(hypotheticalDocument.strip()).build();
    }

    public static final class Builder {
        private ChatClient.Builder chatClientBuilder;
        private PromptTemplate promptTemplate = new PromptTemplate(DEFAULT_PROMPT);

        public Builder chatClientBuilder(ChatClient.Builder chatClientBuilder) {
            this.chatClientBuilder = chatClientBuilder;
            return this;
        }

        public Builder promptTemplate(PromptTemplate promptTemplate) {
            this.promptTemplate = promptTemplate;
            return this;
        }

        public SpringDocHydeQueryTransformer build() {
            return new SpringDocHydeQueryTransformer(this.chatClientBuilder, this.promptTemplate);
        }
    }
}
