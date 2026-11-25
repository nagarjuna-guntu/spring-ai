package com.boardgame.ingestion.batch.writer;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardGameDocumentItemWriter implements ItemWriter<List<Document>> {

    private final VectorStore vectorStore;

    public BoardGameDocumentItemWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void write(Chunk<? extends List<Document>> chunk) throws Exception {

        List<Document> documents = chunk.getItems()
                .stream()
                .flatMap(List::stream)
                .toList();
        vectorStore.add(documents);

    }
}
