package com.rag.rerank.huggingface.config;

import com.rag.rerank.huggingface.config.client.RerankRequest;
import com.rag.rerank.huggingface.config.client.RerankResult;
import com.rag.rerank.huggingface.config.client.TeiRerankClient;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

@NullMarked
@Slf4j
public class TeiRerankingDocumentPostProcessor implements DocumentPostProcessor {
    private static final String METADATA_RERANK_SCORE = "tei_rerank_score";
    private static final double DEFAULT_MIN_SCORE = 0.0;
    private static final int PREVIEW_MAX_LENGTH = 150;

    private final TeiRerankClient rerankClient;
    private final int topK;
    private final double minScore;

    public TeiRerankingDocumentPostProcessor(TeiRerankClient rerankClient, int topK) {
        this(rerankClient, topK, DEFAULT_MIN_SCORE);
    }

    public TeiRerankingDocumentPostProcessor(
            TeiRerankClient rerankClient, int topK, double minScore) {
        this.rerankClient = Objects.requireNonNull(rerankClient, "rerankClient must not be null.");
        Assert.isTrue(topK > 0, "topK must be greater than zero.");
        Assert.isTrue(minScore >= 0.0 && minScore <= 1.0, "minScore must be between 0.0 and 1.0.");
        this.topK = topK;
        this.minScore = minScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static <T, K> Gatherer<T, ?, T> distinctBy(
            Function<? super T, ? extends K> keyExtractor) {
        Supplier<Set<K>> initializer = HashSet::new;
        Gatherer.Integrator<Set<K>, T, T> integrator =
                (state, element, downstream) -> {
                    if (state.add(keyExtractor.apply(element))) {
                        downstream.push(element);
                    }
                    return true;
                };
        return Gatherer.ofSequential(initializer, integrator);
    }

    private List<String> getDocsText(List<Document> documents) {
        return documents.stream()
                .map(document ->
                        switch (document.getText()) {
                            case null -> "";
                            case String text -> text;
                        })
                .toList();
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        Objects.requireNonNull(query, "query must not be null.");
        Objects.requireNonNull(documents, "documents must not be null.");

        List<Document> nonBlankDocs =
                documents.stream()
                        .filter(doc -> doc.getText() != null && !doc.getText().isBlank())
                        .toList();

        if (nonBlankDocs.isEmpty()) {
            return List.of();
        }
        // Only skip when there is a single document AND no minimum score threshold to enforce
        if (nonBlankDocs.size() == 1 && minScore == DEFAULT_MIN_SCORE) {
            return nonBlankDocs;
        }

        log.atDebug()
                .addKeyValue("query", query.text())
                .addKeyValue("candidateCount", nonBlankDocs.size())
                .addKeyValue("targetTopK", topK)
                .log("Starting TEI cross-encoder reranking.");
        try {
            List<String> docsText = getDocsText(nonBlankDocs);

            List<RerankResult> rerankResults = getRerankResults(query, docsText);

            List<Document> reranked = sanitizeRerankedResults(nonBlankDocs, rerankResults);

            log.atInfo()
                    .addKeyValue("inputCount", nonBlankDocs.size())
                    .addKeyValue("rerankedCount", reranked.size())
                    .addKeyValue("rerankedDocIds", reranked.stream().map(Document::getId).toList())
                    .log("Completed TEI cross-encoder reranking.");

            logRerankedResults(reranked);

            return reranked;

        } catch (RuntimeException exception) {
            log.atWarn()
                    .setCause(exception)
                    .addKeyValue("topK", topK)
                    .log("TEI reranking failed; falling back to original RRF order.");
            return documents.stream().limit(topK).toList();
        }
    }

    private String truncate(@Nullable String text) {
        return switch (text) {
            case null -> "";
            case String s when s.length() <= PREVIEW_MAX_LENGTH -> s;
            case String s -> s.substring(0, PREVIEW_MAX_LENGTH) + "...";
        };
    }

    private void logRerankedResults(List<Document> rerankedDocuments) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.atDebug()
                .addKeyValue("stage", "TEI_RERANKED")
                .addKeyValue("count", rerankedDocuments.size())
                .log("Completed TEI document reranking.");

        rerankedDocuments.forEach(doc ->
                log.atDebug()
                        .addKeyValue("score", doc.getMetadata().get(METADATA_RERANK_SCORE))
                        .addKeyValue("docId", doc.getId())
                        .addKeyValue("preview", truncate(doc.getText()))
                        .log("Reranked document."));
    }

    private List<Document> sanitizeRerankedResults(
            List<Document> documents, List<RerankResult> rerankResults) {
        return rerankResults.stream()
                .filter(rerankResult -> isValidEntry(rerankResult, documents))
                .gather(distinctBy(RerankResult::index))
                .filter(rerankResult -> rerankResult.score() >= minScore)
                .sorted(
                        Comparator.comparingDouble(RerankResult::score)
                                .reversed()
                                .thenComparingInt(RerankResult::index))
                .limit(topK)
                .map(rerankResult ->
                        attachScore(documents.get(rerankResult.index()), rerankResult.score()))
                .toList();
    }

    private boolean isValidEntry(RerankResult rerankResult, List<Document> documents) {
        boolean validIndex = rerankResult.index() >= 0 && rerankResult.index() < documents.size();
        boolean validScore = Double.isFinite(rerankResult.score());
        return validIndex && validScore;
    }

    private List<RerankResult> getRerankResults(Query query, List<String> docsText) {
        var rerankResults = rerankClient.rerank(RerankRequest.of(query.text(), docsText, true));

        if (rerankResults.isEmpty()) {
            log.atWarn()
                    .addKeyValue("query", query.text())
                    .log("TEI reranker returned empty results; falling back to RRF order.");

            throw new IllegalStateException("Reranking model returned an empty ranking response.");
        }
        return rerankResults;
    }

    private Document attachScore(Document original, double score) {
        var metadata = new HashMap<>(original.getMetadata());
        metadata.put(METADATA_RERANK_SCORE, score);
        return original.mutate().metadata(metadata).build();
    }

    public static final class Builder {
        private @Nullable TeiRerankClient rerankClient;
        private int topK = 5;
        private double minScore = DEFAULT_MIN_SCORE;

        public Builder rerankClient(TeiRerankClient rerankClient) {
            this.rerankClient =
                    Objects.requireNonNull(rerankClient, "rerankClient must not be null.");
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder minScore(double minScore) {
            this.minScore = minScore;
            return this;
        }

        public TeiRerankingDocumentPostProcessor build() {
            return new TeiRerankingDocumentPostProcessor(
                    Objects.requireNonNull(this.rerankClient, "rerankClient must be configured."),
                    this.topK,
                    this.minScore);
        }
    }
}
