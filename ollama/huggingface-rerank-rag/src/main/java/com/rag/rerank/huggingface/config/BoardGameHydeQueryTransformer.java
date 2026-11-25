package com.rag.rerank.huggingface.config;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class BoardGameHydeQueryTransformer implements QueryTransformer {

    public static final String CONTEXT_GAME_TITLE = "gameTitle";

    private static final String DEFAULT_PROMPT =
            """
            Write a short rulebook excerpt that would appear in the official rulebook
            for the board game "{gameTitle}" to answer the following question.

            Do not mention that the passage is hypothetical.
            Do not explain your reasoning.
            Do not ask follow-up questions.
            Use official rulebook terminology, phase names, card/token terms, and
            mechanics specific to "{gameTitle}".

            Question:
            {query}

            Rulebook excerpt:
            """;

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;

    public BoardGameHydeQueryTransformer(
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

        Object gameTitle = query.context().get(CONTEXT_GAME_TITLE);
        Assert.isTrue(
                gameTitle != null && !gameTitle.toString().isBlank(),
                "Query context must contain a non-empty '" + CONTEXT_GAME_TITLE + "' attribute.");

        log.atDebug()
                .addKeyValue("documentType", "RULEBOOK")
                .addKeyValue("gameTitle", gameTitle)
                .addKeyValue("originalQuery", query.text())
                .log("Transforming query with HyDE.");

        String hypotheticalRulebookExcerpt =
                this.chatClient
                        .prompt()
                        .user(user ->
                                user.text(this.promptTemplate.getTemplate())
                                        .params(
                                                Map.of(
                                                        CONTEXT_GAME_TITLE,
                                                        gameTitle.toString().strip(),
                                                        "query",
                                                        query.text())))
                        .call()
                        .content();
        Assert.hasText(
                hypotheticalRulebookExcerpt,
                "The model did not generate a hypothetical rulebook excerpt.");

        String strippedExcerpt = hypotheticalRulebookExcerpt.strip();

        log.atDebug()
                .addKeyValue("documentType", "RULEBOOK")
                .addKeyValue("gameTitle", gameTitle)
                .addKeyValue("hydeExcerpt", strippedExcerpt)
                .log("Generated HyDE excerpt.");

        return query.mutate().text(strippedExcerpt).build();
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

        public BoardGameHydeQueryTransformer build() {
            return new BoardGameHydeQueryTransformer(this.chatClientBuilder, this.promptTemplate);
        }
    }
}
