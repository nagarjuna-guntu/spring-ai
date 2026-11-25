package com.rag.ingestion.batch.gamedocs;

import com.rag.ingestion.config.gamedocs.BoardGameDocsProperties;
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
import java.util.*;
import java.util.stream.Stream;

@Component
@StepScope
@Slf4j
@NullMarked
public class BoardGameFileItemReader implements ItemStreamReader<BoardGameFile> {

    private static final String LAST_PROCESSED_PATH = "boardGameFileReader.lastProcessedPath";
    private static final IngestionDomain BOARD_GAMES_DOMAIN = IngestionDomain.BOARD_GAMES;

    private final BoardGameDocsProperties boardGameDocsProperties;
    private final FileMetadataService fileMetadataService;
    private final IngestedFileLogTrackingService ingestedFileLogTrackingService;

    private List<BoardGameFile> candidateFiles = Collections.emptyList();
    private int currentIndex;
    @Nullable private String lastProcessedPath;

    public BoardGameFileItemReader(
            BoardGameDocsProperties boardGameDocsProperties,
            FileMetadataService fileMetadataService,
            IngestedFileLogTrackingService ingestedFileLogTrackingService) {

        this.boardGameDocsProperties = boardGameDocsProperties;
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
                    "Unable to scan Board Games documentation directory: " + rootDirectory,
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
    public @Nullable BoardGameFile read() {
        if (currentIndex >= candidateFiles.size()) {
            return null;
        }
        BoardGameFile nextFile = candidateFiles.get(currentIndex++);
        this.lastProcessedPath = nextFile.metadata().filePath();

        return nextFile;
    }

    private List<BoardGameFile> discoverCandidateFiles(Path rootDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .map(path -> getMetaData(rootDirectory, path))
                    .filter(this::needsIngestion)
                    .sorted(Comparator.comparing(FileMetadata::filePath))
                    .map(this::toBoardGameFile)
                    .toList();
        }
    }

    private BoardGameFile toBoardGameFile(FileMetadata metadata) {
        Path absolutePath = metadata.absolutePath();
        String gameId = extractGameId(absolutePath);
        String fileName = absolutePath.getFileName().toString();
        return BoardGameFile.of(metadata, gameId, fileName);
    }

    private String extractGameId(Path absolutePath) {
        Path parent = absolutePath.getParent();
        return parent.getFileName().toString();
    }

    private boolean needsIngestion(FileMetadata fileMetadata) {
        return ingestedFileLogTrackingService.needsIngestion(fileMetadata);
    }

    private FileMetadata getMetaData(Path rootDirectory, Path file) {
        return fileMetadataService.getMetadata(BOARD_GAMES_DOMAIN, rootDirectory, file);
    }

    private boolean isSupportedFile(Path path) {

        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".txt");
    }

    // Restore state if restarting a failed or stopped job, Checks the last processed path
    private @Nullable String restoreCheckPoint(ExecutionContext executionContext) {

        String checkpoint =
                executionContext.containsKey(LAST_PROCESSED_PATH)
                        ? executionContext.getString(LAST_PROCESSED_PATH)
                        : null;

        if (checkpoint != null) {
            log.atInfo()
                    .addKeyValue("domain", BOARD_GAMES_DOMAIN)
                    .addKeyValue("lastCheckpoint", checkpoint)
                    .addKeyValue("pendingFiles", candidateFiles.size())
                    .log("Restarting BoardGameFileItemReader from previous checkpoint.");
        } else {
            log.atInfo()
                    .addKeyValue("domain", BOARD_GAMES_DOMAIN)
                    .addKeyValue("pendingFiles", candidateFiles.size())
                    .log("Initialized BoardGameFileItemReader with new/modified files.");
        }

        return checkpoint;
    }

    private Path resolveRootDirectory() {
        Path rootDirectory = boardGameDocsProperties.inputDirectory().toAbsolutePath().normalize();

        if (!Files.isDirectory(rootDirectory)) {
            throw new ItemStreamException(
                    "Board Games Documentation directory does not exist: " + rootDirectory);
        }
        return rootDirectory;
    }
}
