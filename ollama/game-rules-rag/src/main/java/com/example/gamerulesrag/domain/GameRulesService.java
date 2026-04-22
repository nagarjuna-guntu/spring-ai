package com.example.gamerulesrag.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameRulesService {
    private final VectorStore vectorStore;

    public GameRulesService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String findRulesFor(String gameTitle, String question) {
        Filter.Expression gameTitleFilterExpression =
                new FilterExpressionBuilder()
                        .eq("gameTitle", normalizedTitle(gameTitle))
                        .build();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                //.similarityThreshold(0.5) //default is 0.0, range is 0.0 - 1.0
                .filterExpression(gameTitleFilterExpression)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        if (documents.isEmpty()) {
            log.info("No similar documents are available for the game : {} .", normalizedTitle(gameTitle));
            return String.format("The rules for %s are not available.", gameTitle);
        }

        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

    }

    public String normalizedTitle(String title) {
        return title.toLowerCase().replace(" ", "_");
    }
}

