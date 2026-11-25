package com.rag.ingestion.batch.springdocs;

import com.rag.ingestion.batch.common.DocumentMapper;
import com.rag.ingestion.config.springdocs.SpringBootDocsProperties;
import com.rag.ingestion.domain.file.FileIngestionException;
import com.rag.ingestion.domain.file.FileIngestionResult;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
public class SpringBootDocsItemProcessor
        implements ItemProcessor<SpringBootDocFile, FileIngestionResult> {

    public static final String SOURCE_FILE_PATH = "sourceFilePath";
    public static final String SOURCE_ID_PREFIX = "spring-boot:";

    private final TokenTextSplitter tokenTextSplitter;
    private final SpringBootDocsProperties springBootDocsProperties;
    private final DocumentMapper documentMapper;

    private final Timer documentReadTimer;
    private final Timer documentSplitTimer;

    public SpringBootDocsItemProcessor(
            TokenTextSplitter tokenTextSplitter,
            SpringBootDocsProperties springBootDocsProperties,
            DocumentMapper documentMapper,
            MeterRegistry meterRegistry) {
        this.tokenTextSplitter = tokenTextSplitter;
        this.springBootDocsProperties = springBootDocsProperties;
        this.documentMapper = documentMapper;

        this.documentReadTimer =
                Timer.builder("rag.ingestion.document.read")
                        .description("Time spent reading documentation files")
                        .tag("domain", "spring-boot-docs")
                        .register(meterRegistry);
        this.documentSplitTimer =
                Timer.builder("rag.ingestion.document.split")
                        .description("Time spent splitting documentation files")
                        .tag("domain", "spring-boot-docs")
                        .register(meterRegistry);
    }

    @Override
    public @Nullable FileIngestionResult process(SpringBootDocFile file) {
        List<Document> chunks = extractAndSplitDocuments(file);
        String sourceId = generateSourceId(file);
        var sourceMetadata = buildMetadata(file);
        // 3. Add source metadata to each chunk
        var enrichedChunks = documentMapper.toDocuments(chunks, sourceMetadata, sourceId);

        log.atInfo()
                .addKeyValue("filePath", file.path().normalize())
                .addKeyValue("fileName", file.fileName())
                .addKeyValue("chunkCount", chunks.size())
                .addKeyValue("sourceId", sourceId)
                .addKeyValue("enrichedChunks", enrichedChunks.size())
                .log("File has been processed successfully.");

        return FileIngestionResult.of(file.metadata(), enrichedChunks);
    }

    private String generateSourceId(SpringBootDocFile file) {
        return SOURCE_ID_PREFIX + file.metadata().filePath().toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> buildMetadata(SpringBootDocFile file) {
        return Map.of(
                SOURCE_FILE_PATH,
                file.metadata().filePath(),
                "framework",
                springBootDocsProperties.framework(),
                "version",
                springBootDocsProperties.version(),
                "fileName",
                file.fileName(),
                "filePath",
                file.path().toString(),
                "documentType",
                "SPRING_BOOT_DOC");
    }

    private List<Document> extractAndSplitDocuments(SpringBootDocFile file) {
        var resource = new FileSystemResource(file.path());
        TikaDocumentReader documentReader = new TikaDocumentReader(resource);
        List<Document> documents = documentReadTimer.record(documentReader::read);
        List<Document> chunks = documentSplitTimer.record(() -> tokenTextSplitter.split(documents));
        if (chunks.isEmpty()) {
            throw new FileIngestionException(
                    "No document chunks generated for file: " + file.metadata().filePath());
        }
        return chunks;
    }
}
