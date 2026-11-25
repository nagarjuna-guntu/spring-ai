package com.rag.decision.ollama.domain.games;


import com.rag.decision.ollama.config.games.BoardGameHydeQueryTransformer;
import com.rag.decision.ollama.domain.RagSessionIdFactory;
import com.rag.decision.ollama.web.AssistantResponse;
import com.rag.decision.ollama.web.games.BoardGameQuestion;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {
    private static final String SYSTEM_PROMPT =
            """
            You are a board-game rules assistant.

            Answer questions using only the retrieved rulebook content.

            Instructions:
            1. Explain the rules clearly and step by step.
            2. Preserve the exact meaning of the game rules.
            3. Do not invent rules, actions, penalties, or game mechanics.
            4. If the retrieved context does not contain the answer,
               clearly say that you could not find the answer in the rulebook.
            5. When explaining a player's turn, present the actions
               in the correct sequence if the rulebook specifies one.
            6. Use simple language suitable for someone learning the game.
            """;

    private final ChatClient chatClient;
    private final Advisor boardGameRagAdvisor;
    private final RagSessionIdFactory sessionIdFactory;

    public BoardGameService(
            ChatClient chatClient,
            @Qualifier("boardGameRagAdvisor") Advisor boardGameRagAdvisor,
            RagSessionIdFactory sessionIdFactory) {
        this.chatClient = chatClient;
        this.boardGameRagAdvisor = boardGameRagAdvisor;
        this.sessionIdFactory = sessionIdFactory;
    }

    public AssistantResponse ask(BoardGameQuestion question, String loggedInUser) {
        var filterExpression = buildFilterExpression(question);
        String sessionId = sessionIdFactory.forBoardGame(loggedInUser, question.gameId());

        log.atInfo()
                .addKeyValue("documentType", "RULEBOOK")
                .addKeyValue("question", question)
                .addKeyValue("filterExpression", filterExpression)
                .addKeyValue("session-id", sessionId)
                .log("Asking question.");

        return chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(question.question())
                .advisors(boardGameRagAdvisor)
                .advisors(advisorSpec ->
                        advisorSpec
                                .param(
                                        VectorStoreDocumentRetriever.FILTER_EXPRESSION,
                                        filterExpression)
                                .param(
                                        BoardGameHydeQueryTransformer.CONTEXT_GAME_TITLE,
                                        question.gameId()))
                .advisors(SessionAdvisors.session(sessionId))
                .call()
                .entity(
                        AssistantResponse.class,
                        entityParamSpec ->
                                entityParamSpec.validateSchema().useProviderStructuredOutput());
    }

    private Filter.Expression buildFilterExpression(BoardGameQuestion question) {
        var filter = new FilterExpressionBuilder();
        return filter.and(
                        filter.eq("documentType", "RULEBOOK"),
                        filter.eq("gameId", question.normalizeTitle()))
                .build();
    }
}
