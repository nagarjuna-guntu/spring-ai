package com.rag.ingestion.batch.springdocs;

import com.rag.ingestion.config.springdocs.SpringBootDocsProperties;
import com.rag.ingestion.domain.common.IngestionDomain;
import com.rag.ingestion.domain.file.FileMetadata;
import com.rag.ingestion.domain.file.FileMetadataService;
import com.rag.ingestion.domain.tracking.IngestedFileLogTrackingService;

import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
@StepScope
@Slf4j
@NullMarked
public class SpringBootDocsItemReader implements ItemStreamReader<SpringBootDocFile> {

    private static final String LAST_PROCESSED_PATH =
            "springBootDocsReader.lastProcessedPath"; // enable restartability
    private static final IngestionDomain SPRING_BOOT_DOCS_DOMAIN = IngestionDomain.SPRING_BOOT_DOCS;

    private final SpringBootDocsProperties springBootDocsProperties;
    private final FileMetadataService fileMetadataService;
    private final IngestedFileLogTrackingService ingestedFileLogTrackingService;

    private int currentIndex;
    private List<SpringBootDocFile> candidateFiles = Collections.emptyList();
    private @Nullable String lastProcessedPath;

    public SpringBootDocsItemReader(
            SpringBootDocsProperties springBootDocsProperties,
            FileMetadataService fileMetadataService,
            IngestedFileLogTrackingService ingestedFileLogTrackingService) {
        this.springBootDocsProperties = springBootDocsProperties;
        this.fileMetadataService = fileMetadataService;
        this.ingestedFileLogTrackingService = ingestedFileLogTrackingService;
    }

    @Override
    public void open(ExecutionContext executionContext) {

        // Initialize the list of files and current index
        Path rootDirectory = resolveRootDirectory();

        try {
            // Discover only new or modified files in deterministic path order
            this.candidateFiles = discoverCandidateFiles(rootDirectory);
            this.currentIndex = 0;
            this.lastProcessedPath = restoreCheckPoint(executionContext);

        } catch (IOException exception) {
            throw new ItemStreamException(
                    "Unable to scan Spring Boot documentation directory: " + rootDirectory,
                    exception);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (lastProcessedPath != null) {
            executionContext.putString(LAST_PROCESSED_PATH, lastProcessedPath);
        }
    }

    @Override
    public void close() {
        this.candidateFiles = Collections.emptyList();
        this.currentIndex = 0;
        this.lastProcessedPath = null;
    }

    @Override
    public @Nullable SpringBootDocFile read() {

        if (currentIndex >= candidateFiles.size()) {
            return null;
        }

        SpringBootDocFile nextFile = candidateFiles.get(currentIndex++);
        this.lastProcessedPath = nextFile.metadata().filePath();
        return nextFile;
    }

    private @Nullable String restoreCheckPoint(ExecutionContext executionContext) {
        String checkpoint =
                executionContext.containsKey(LAST_PROCESSED_PATH)
                        ? executionContext.getString(LAST_PROCESSED_PATH)
                        : null;

        if (checkpoint != null) {
            log.atInfo()
                    .addKeyValue("domain", SPRING_BOOT_DOCS_DOMAIN)
                    .addKeyValue("lastCheckpoint", checkpoint)
                    .addKeyValue("pendingFiles", candidateFiles.size())
                    .log("Restarting SpringBootDocsItemReader from previous checkpoint.");
        } else {
            log.atInfo()
                    .addKeyValue("domain", SPRING_BOOT_DOCS_DOMAIN)
                    .addKeyValue("pendingFiles", candidateFiles.size())
                    .log("Initialized SpringBootDocsItemReader with new/modified files.");
        }
        return checkpoint;
    }

    private Path resolveRootDirectory() {
        Path rootDirectory = springBootDocsProperties.rootDirectory().toAbsolutePath().normalize();

        if (!Files.isDirectory(rootDirectory)) {
            throw new ItemStreamException(
                    "Documentation directory does not exist: " + rootDirectory);
        }
        return rootDirectory;
    }

    private List<SpringBootDocFile> discoverCandidateFiles(Path rootDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .map(file -> getFileMetaData(rootDirectory, file))
                    .filter(this::needsIngestion)
                    .sorted(Comparator.comparing(FileMetadata::filePath))
                    .map(SpringBootDocFile::of)
                    .toList();
        }
    }

    private FileMetadata getFileMetaData(Path rootDirectory, Path file) {
        return fileMetadataService.getMetadata(SPRING_BOOT_DOCS_DOMAIN, rootDirectory, file);
    }

    private boolean needsIngestion(FileMetadata fileMetadata) {
        return ingestedFileLogTrackingService.needsIngestion(fileMetadata);
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return name.endsWith(".pdf")
                || name.endsWith(".docx")
                || name.endsWith(".txt")
                || name.endsWith(".adoc");
    }
}
