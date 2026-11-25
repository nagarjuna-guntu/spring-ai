package com.rag.rerank.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@NullMarked
public class ReciprocalRankFusionPreservingDocumentJoiner implements DocumentJoiner {

    @Override
    public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
        return documentsForQuery.values().stream()
                .flatMap(List::stream)
                .flatMap(List::stream)
                .toList();
    }
}
