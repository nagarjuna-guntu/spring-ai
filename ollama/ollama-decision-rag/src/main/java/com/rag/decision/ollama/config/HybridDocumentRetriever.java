package com.rag.decision.ollama.config;

import com.rag.decision.ollama.config.lucene.LuceneFilterQueryMapper;
import com.rag.decision.ollama.domain.LuceneSearchService;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

@NullMarked
@Slf4j
public class HybridDocumentRetriever implements DocumentRetriever {

    private static final int RRF_CONSTANT_K = 60;

    private final VectorStore vectorStore;
    private final LuceneSearchService luceneSearch;
    private final LuceneFilterQueryMapper luceneFilterQueryMapper;
    private final int topK;

    public HybridDocumentRetriever(
            VectorStore vectorStore,
            LuceneSearchService luceneSearch,
            LuceneFilterQueryMapper luceneFilterQueryMapper,
            int topK) {

        this.vectorStore = Objects.requireNonNull(vectorStore);
        this.luceneSearch = Objects.requireNonNull(luceneSearch);
        this.luceneFilterQueryMapper = Objects.requireNonNull(luceneFilterQueryMapper);
        Assert.isTrue(topK > 0, "topK must be greater than zero");
        this.topK = topK;
    }

    @Override
    @SuppressWarnings("preview")
    public List<Document> retrieve(org.springframework.ai.rag.Query query) {

        Objects.requireNonNull(query, "query must not be null");

        Filter.@Nullable Expression filter = extractFilterExpression(query);
        org.apache.lucene.search.Query luceneFilter = luceneFilterQueryMapper.toLuceneQuery(filter);

        log.atDebug()
                .addKeyValue("queryText", query.text())
                .addKeyValue("filter", filter)
                .addKeyValue(
                        "luceneFilter", luceneFilter != null ? luceneFilter.toString() : "none")
                .addKeyValue("topK", topK)
                .log("Starting hybrid document retrieval.");

        try (var scope =
                StructuredTaskScope.open(
                        StructuredTaskScope.Joiner.<List<Document>>allSuccessfulOrThrow())) {

            var vectorTask = scope.fork(() -> vectorStoreSearch(query.text(), filter));
            var luceneTask = scope.fork(() -> luceneSearch(query.text(), luceneFilter));

            List<List<Document>> result = scope.join();

            List<Document> fused = fuseWithRrf(result, topK);

            log.atInfo()
                    .addKeyValue("vectorHits", vectorTask.get().size())
                    .addKeyValue("luceneHits", luceneTask.get().size())
                    .addKeyValue("fusedCount", fused.size())
                    .addKeyValue("fusedDocIds", fused.stream().map(Document::getId).toList())
                    .log("Completed hybrid document retrieval.");

            return fused;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.atError().setCause(exception).log("Hybrid document retrieval was interrupted.");
            throw new IllegalStateException(
                    "Hybrid document retrieval was interrupted.", exception);
        } catch (ExecutionException exception) {
            log.atError()
                    .setCause(exception.getCause())
                    .log("One or more hybrid search backends failed.");
            throw new IllegalStateException(
                    "One or more hybrid search backends failed.", exception.getCause());
        }
    }

    private List<Document> vectorStoreSearch(String queryText, Filter.@Nullable Expression filter) {
        var docs = vectorStore.similaritySearch(buildSearchRequest(queryText, filter));
        log.atDebug()
                .addKeyValue("backend", "pgvector")
                .addKeyValue("hitCount", docs.size())
                .addKeyValue("docIds", docs.stream().map(Document::getId).toList())
                .log("Vector store search completed.");
        return docs;
    }

    private List<Document> luceneSearch(
            String queryText, org.apache.lucene.search.Query luceneFilter) {
        var docs = luceneSearch.search(queryText, topK, luceneFilter);
        log.atDebug()
                .addKeyValue("backend", "lucene")
                .addKeyValue("hitCount", docs.size())
                .addKeyValue("docIds", docs.stream().map(Document::getId).toList())
                .log("Lucene search completed.");

        return docs;
    }

    private Filter.@Nullable Expression extractFilterExpression(
            org.springframework.ai.rag.Query query) {

        Object filterValue = query.context().get(VectorStoreDocumentRetriever.FILTER_EXPRESSION);

        return switch (filterValue) {
            case null -> null;
            case Filter.Expression expression -> expression;
            case String expression when expression.isBlank() -> null;
            case String expression -> new FilterExpressionTextParser().parse(expression);
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported filter expression type: "
                                    + filterValue.getClass().getName());
        };
    }

    private SearchRequest buildSearchRequest(String queryText, Filter.@Nullable Expression filter) {

        var searchRequest = SearchRequest.builder().query(queryText).topK(topK);
        if (filter != null) {
            searchRequest.filterExpression(filter);
        }
        return searchRequest.build();
    }

    private List<Document> fuseWithRrf(List<List<Document>> rankedLists, int limit) {

        record ScoredDocument(String documentId, Document document, double score) {

            ScoredDocument addScore(double additionalScore) {
                return new ScoredDocument(documentId, document, score + additionalScore);
            }
        }

        Map<String, ScoredDocument> fusedById = new HashMap<>();

        for (List<Document> results : rankedLists) {
            for (int index = 0; index < results.size(); index++) {
                Document document = results.get(index);
                String documentId = document.getId();
                Assert.hasText(documentId, "Retrieved document ID must not be null.");

                double rrfScore = 1.0 / (RRF_CONSTANT_K + index + 1);

                fusedById.compute(
                        documentId,
                        (_, existing) ->
                                existing == null
                                        ? new ScoredDocument(documentId, document, rrfScore)
                                        : existing.addScore(rrfScore));
            }
        }

        var fusedResult =
                fusedById.values().stream()
                        .sorted(
                                Comparator.comparingDouble(ScoredDocument::score)
                                        .reversed()
                                        .thenComparing(ScoredDocument::documentId))
                        .limit(limit)
                        .map(ScoredDocument::document)
                        .toList();

        log.atDebug()
                .addKeyValue("result", "fused")
                .addKeyValue("resultCount", fusedResult.size())
                .addKeyValue("docIds", fusedResult.stream().map(Document::getId).toList())
                .log("Fused Result Calculated.");

        return fusedResult;
    }
}
