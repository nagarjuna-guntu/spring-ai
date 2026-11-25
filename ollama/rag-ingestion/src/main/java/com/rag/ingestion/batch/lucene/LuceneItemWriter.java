package com.rag.ingestion.batch.lucene;

import com.rag.ingestion.batch.common.DocumentMapper;
import com.rag.ingestion.domain.file.FileIngestionResult;

import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@NullMarked
@Slf4j
public class LuceneItemWriter implements ItemWriter<FileIngestionResult> {

    private static final String DOCUMENT_ID = "documentId";
    private static final String CONTENT = "content";
    private static final String SOURCE_ID = "sourceId";

    private static final Set<String> RESERVED_FIELDS = Set.of(DOCUMENT_ID, CONTENT, SOURCE_ID);

    private final IndexWriter indexWriter;
    private final DocumentMapper documentMapper;

    public LuceneItemWriter(IndexWriter indexWriter, DocumentMapper documentMapper) {

        Assert.notNull(indexWriter, "IndexWriter must not be null");
        Assert.notNull(documentMapper, "DocumentMapper must not be null");

        this.indexWriter = indexWriter;
        this.documentMapper = documentMapper;
    }

    @Override
    public void write(Chunk<? extends FileIngestionResult> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        for (FileIngestionResult fileIngestionResult : chunk) {
            indexFile(fileIngestionResult);
        }

        indexWriter.commit();

        log.atDebug()
                .addKeyValue("indexedFileCount", chunk.size())
                .log("Committed Lucene index chunk.");
    }

    private void indexFile(FileIngestionResult fileIngestionResult) throws IOException {

        Assert.notNull(fileIngestionResult, "File ingestion result must not be null");
        var sourceDocuments = fileIngestionResult.documents();
        Assert.notEmpty(sourceDocuments, "Documents cannot be empty for Lucene indexing");
        String expectedSourceId = documentMapper.sourceIdOf(sourceDocuments.getFirst());

        List<Document> luceneDocuments =
                sourceDocuments.stream()
                        .map(sourceDocument -> toLuceneDocument(sourceDocument, expectedSourceId))
                        .toList();
        // Atomically replaces all previous Lucene documents matching this sourceId
        indexWriter.updateDocuments(new Term(SOURCE_ID, expectedSourceId), luceneDocuments);
    }

    private Document toLuceneDocument(
            org.springframework.ai.document.Document sourceDocument, String expectedSourceId) {

        String actualSourceId = documentMapper.sourceIdOf(sourceDocument);
        Assert.isTrue(
                actualSourceId.equals(expectedSourceId),
                () ->
                        "All chunks must belong to the same source file. Expected [%s], found [%s]"
                                .formatted(expectedSourceId, actualSourceId));
        String content = sourceDocument.getText();
        Assert.hasText(content, "Document content cannot be empty");
        var luceneDocument = new Document();
        luceneDocument.add(new StringField(DOCUMENT_ID, sourceDocument.getId(), Field.Store.YES));
        luceneDocument.add(new TextField(CONTENT, content, Field.Store.YES));
        luceneDocument.add(new StringField(SOURCE_ID, actualSourceId, Field.Store.YES));

        addMetadataFields(luceneDocument, sourceDocument.getMetadata());

        return luceneDocument;
    }

    private void addMetadataFields(
            Document luceneDocument, Map<String, @Nullable Object> metadata) {
        metadata.forEach((key, value) -> {
            if (value != null && !RESERVED_FIELDS.contains(key)) {
                luceneDocument.add(new StringField(key, value.toString(), Field.Store.YES));
            }
        });
    }
}
