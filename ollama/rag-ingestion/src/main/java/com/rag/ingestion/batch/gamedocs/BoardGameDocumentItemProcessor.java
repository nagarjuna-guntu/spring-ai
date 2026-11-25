package com.rag.ingestion.batch.gamedocs;

import com.rag.ingestion.batch.common.DocumentMapper;
import com.rag.ingestion.domain.file.FileIngestionException;
import com.rag.ingestion.domain.file.FileIngestionResult;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@NullMarked
@Slf4j
public class BoardGameDocumentItemProcessor
        implements ItemProcessor<BoardGameFile, FileIngestionResult> {

    public static final String SOURCE_FILE_PATH = "sourceFilePath";
    public static final String SOURCE_ID_PREFIX = "board-games:";

    private final TokenTextSplitter tokenTextSplitter;
    private final DocumentMapper documentMapper;

    public BoardGameDocumentItemProcessor(
            TokenTextSplitter tokenTextSplitter, DocumentMapper documentMapper) {
        this.tokenTextSplitter = tokenTextSplitter;
        this.documentMapper = documentMapper;
    }

    @Override
    public @Nullable FileIngestionResult process(BoardGameFile file) {
        // Implementation for processing the board game file and splitting it into documents
        String normalizedGameID = requireValidGameId(file);
        List<Document> chunks = extractAndSplitDocuments(file);
        String sourceId = generateSourceId(file);
        var baseMetadata = buildBaseMetadata(file, normalizedGameID);
        var enrichedChunks = documentMapper.toDocuments(chunks, baseMetadata, sourceId);

        log.atInfo()
                .addKeyValue("filePath", file.path().normalize())
                .addKeyValue("fileName", file.fileName())
                .addKeyValue("normalizedGameID", normalizedGameID)
                .addKeyValue("chunkCount", chunks.size())
                .addKeyValue("sourceId", sourceId)
                .addKeyValue("enrichedChunks", enrichedChunks.size())
                .log("File has been processed successfully.");

        return FileIngestionResult.of(file.metadata(), enrichedChunks);
    }

    private String generateSourceId(BoardGameFile file) {
        return SOURCE_ID_PREFIX + file.metadata().filePath().toLowerCase(Locale.ROOT);
    }

    private List<Document> extractAndSplitDocuments(BoardGameFile file) {
        var resource = new FileSystemResource(file.path());
        var documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReader.read();
        List<Document> chunks = tokenTextSplitter.split(documents);
        if (chunks.isEmpty()) {
            throw new FileIngestionException(
                    "No document chunks generated for file: " + file.metadata().filePath());
        }

        return chunks;
    }

    private Map<String, Object> buildBaseMetadata(BoardGameFile file, String normalizedGameID) {
        return Map.of(
                SOURCE_FILE_PATH,
                file.metadata().filePath(),
                "gameId",
                normalizedGameID,
                "fileName",
                file.fileName(),
                "documentType",
                "RULEBOOK");
    }

    private String requireValidGameId(BoardGameFile boardGameFile) {
        var normalizedId = boardGameFile.normalizedGameId();
        if (normalizedId.isBlank()) {
            throw new FileIngestionException(
                    "Invalid game ID after normalization: " + boardGameFile.gameId());
        }
        return normalizedId;
    }
}
