package com.rag.rerank.huggingface.domain;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;

@NullMarked
public class LuceneSearchService implements Closeable {

    private static final String FIELD_DOCUMENT_ID = "documentId";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_BM25_SCORE = "bm25_score";

    private final Directory directory;
    private final Analyzer analyzer;
    private final SearcherManager searcherManager;

    public LuceneSearchService(Path indexPath) throws IOException {
        this.directory = FSDirectory.open(indexPath);
        this.analyzer = new StandardAnalyzer();
        // Read-only SearcherManager: never acquires write.lock from the ingestion app.
        this.searcherManager = new SearcherManager(this.directory, null);
    }

    /**
     * Main entry point: runs a BM25 keyword search and maps hits to Spring AI Documents. * Entry
     * point: validates inputs via switch expression and runs BM25 search.
     */
    public List<Document> search(@Nullable String queryText, int topK, Query metadataFilter) {

        Assert.notNull(metadataFilter, "metadataFilter must not be null");
        if (queryText == null || queryText.isBlank() || topK <= 0) {
            return List.of();
        }

        return parseQuery(queryText)
                .map(query -> applyMetadataFilter(query, metadataFilter))
                .map(finalQuery -> executeSearch(finalQuery, topK))
                .orElseGet(List::of);
    }

    /**
     * Step 1: Converts raw user text into a safe Lucene Query. Returns Optional.empty() if the
     * query contains only stop words (like "the", "a", "is").
     */
    private Optional<Query> parseQuery(String rawText) {
        try {
            var parser = new QueryParser(FIELD_CONTENT, analyzer);
            Query query = parser.parse(QueryParser.escape(rawText));

            return switch (query) {
                case MatchNoDocsQuery _ -> Optional.empty();
                case Query validQuery -> Optional.of(validQuery);
            };
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse Lucene query: " + rawText, e);
        }
    }

    private Query applyMetadataFilter(Query textQuery, Query metadataFilter) {
        return switch (metadataFilter) {
            case MatchAllDocsQuery _ -> textQuery;
            case Query filter ->
                    new BooleanQuery.Builder()
                            .add(textQuery, BooleanClause.Occur.MUST)
                            .add(filter, BooleanClause.Occur.FILTER)
                            .build();
        };
    }

    /** Step 2: Acquires a thread-safe IndexSearcher, runs BM25 scoring, and converts hits. */
    private List<Document> executeSearch(Query query, int topK) {
        return withSearcher(searcher -> {
            searcher.setSimilarity(new BM25Similarity()); // sets by default
            TopDocs topDocs = searcher.search(query, topK);
            return Arrays.stream(topDocs.scoreDocs)
                    .map(scoreDoc -> toSpringDocument(searcher, scoreDoc))
                    .flatMap(Optional::stream)
                    .toList();
        });
    }

    /** Step 3: Converts a single Lucene hit (ScoreDoc) into a Spring AI Document. */
    private Optional<Document> toSpringDocument(IndexSearcher searcher, ScoreDoc scoreDoc) {
        try {
            org.apache.lucene.document.Document storedDocument =
                    searcher.storedFields().document(scoreDoc.doc);
            var storedChunk =
                    new StoredChunk(
                            storedDocument.get(FIELD_DOCUMENT_ID),
                            storedDocument.get(FIELD_CONTENT));

            return switch (storedChunk) {
                case StoredChunk(String id, String content)
                        when StringUtils.hasText(id) && StringUtils.hasText(content) ->
                        Optional.of(
                                Document.builder()
                                        .id(id)
                                        .text(content)
                                        .metadata(
                                                extractMetadata(
                                                        storedDocument.getFields(), scoreDoc.score))
                                        .build());
                default -> Optional.empty();
            };
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read stored Lucene document", e);
        }
    }

    /** Step 4: Copies all extra fields (page number, source file, etc.) into Spring AI metadata. */
    private Map<String, Object> extractMetadata(List<IndexableField> fields, float bm25Score) {
        Map<String, Object> metadata = new HashMap<>();

        for (IndexableField field : fields) {
            switch (field.name()) {
                case FIELD_DOCUMENT_ID, FIELD_CONTENT -> {
                    /* Already mapped to Document id and text */
                }
                case String metadataKey when field.stringValue() != null ->
                        metadata.put(metadataKey, field.stringValue());
                default -> {
                    /* Ignore binary or null-valued stored fields */
                }
            }
        }

        // Keep the retrieval score separate from the original ingestion metadata.
        metadata.put(FIELD_BM25_SCORE, bm25Score);
        return metadata;
    }

    /** Resource-leasing helper: safely acquires and always releases the IndexSearcher. */
    private <T> T withSearcher(SearcherCallback<T> action) {
        try {
            searcherManager.maybeRefresh();
            IndexSearcher searcher = searcherManager.acquire();
            try {
                return action.execute(searcher);
            } finally {
                searcherManager.release(searcher);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Lucene BM25 search failed", exception);
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.close(searcherManager, directory, analyzer);
    }

    @FunctionalInterface
    private interface SearcherCallback<T> {
        T execute(IndexSearcher indexSearcher) throws IOException;
    }

    private record StoredChunk(String id, String content) {}
}
