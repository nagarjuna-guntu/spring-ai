package com.example.boardgamebuddy.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameRulesService {
    private final VectorStore vectorStore;

    public GameRulesService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String getRulesFor(String gameName, String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .filterExpression(new FilterExpressionBuilder().eq("gameTitle", normalizeGameTitle(gameName)).build())
                .build();
        List<Document> documentList = vectorStore.similaritySearch(searchRequest);
        if (documentList.isEmpty()) {
            return "The rules for " + gameName + " are not available.";
        }
        return documentList.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String normalizeGameTitle(String gameName) {
        return gameName.toLowerCase().replace(" ", "_");
    }
}
