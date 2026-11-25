package com.rag.ingestion.batch.gamedocs;

import com.rag.ingestion.domain.file.FileIngestionResult;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NullMarked
@Slf4j
public class BoardGameDocumentItemWriter implements ItemWriter<FileIngestionResult> {

    public static final String SOURCE_FILE_PATH = "sourceFilePath";

    private final VectorStore vectorStore;

    public BoardGameDocumentItemWriter(@Qualifier("boardGameVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void write(Chunk<? extends FileIngestionResult> chunk) {
        chunk.forEach(this::replaceExistingDocuments);
    }

    private void replaceExistingDocuments(FileIngestionResult fileIngestionResult) {
        String sourceFilePath = fileIngestionResult.metadata().filePath();
        List<Document> documents = fileIngestionResult.documents();
        deleteExistingDocuments(sourceFilePath);
        log.atInfo()
                .addKeyValue(SOURCE_FILE_PATH, sourceFilePath)
                .addKeyValue("docCount", documents.size())
                .log("Documents added successfully to the Vector store.");
        vectorStore.add(documents);
    }

    private void deleteExistingDocuments(String sourceFilePath) {
        var filter = new FilterExpressionBuilder().eq(SOURCE_FILE_PATH, sourceFilePath).build();

        log.atInfo()
                .addKeyValue(SOURCE_FILE_PATH, sourceFilePath)
                .log("Existing file has been deleted successfully for re-vectorizing");

        vectorStore.delete(filter);
    }
}
