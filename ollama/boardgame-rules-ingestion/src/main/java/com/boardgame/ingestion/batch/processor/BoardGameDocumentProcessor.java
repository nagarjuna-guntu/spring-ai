package com.boardgame.ingestion.batch.processor;

import com.boardgame.ingestion.domain.BoardGameFile;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardGameDocumentProcessor implements
        ItemProcessor<BoardGameFile, List<Document>> {

    private final TokenTextSplitter tokenTextSplitter;

    public BoardGameDocumentProcessor(TokenTextSplitter tokenTextSplitter) {
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @Override
    public @Nullable List<Document> process(BoardGameFile file) throws Exception {
        // Implementation for processing the board game file and splitting it into documents
        var resource = new FileSystemResource(file.path());
        var documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReader.read();
        List<Document> chunks = tokenTextSplitter.split(documents);
        for (int index = 0; index < chunks.size(); index++) {
            Document chunk = chunks.get(index);
            chunk.getMetadata().put("gameId", file.normalizedGameId());
            chunk.getMetadata().put("fileName", file.fileName());
            chunk.getMetadata().put("documentType", "RULEBOOK");
            chunk.getMetadata().put("chunkIndex", String.valueOf(index));
        }
        return chunks.isEmpty() ? null : chunks;
    }

}
