package com.boardgame.ingestion.batch.reader;

import com.boardgame.ingestion.domain.BoardGameFile;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
@StepScope
public class BoardGameFileItemReader implements ItemStreamReader<BoardGameFile> {

    private static final String CURRENT_INDEX = "current.file.index";

    private final String inputDirectory;

    private int currentIndex;
    private List<Path> files;

    public BoardGameFileItemReader(
            @Value("${app.ingestion.input-directory}") String inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        // Initialize the list of files and current index
        try {
            Path root = Path.of(inputDirectory);

            try (Stream<Path> paths = Files.walk(root)) {
                files = paths
                        .filter(Files::isRegularFile)
                        .filter(this::isSupportedFile)
                        .sorted()
                        .toList();
            }

            // Restore the position when restarting a failed job.
            currentIndex = executionContext.getInt(CURRENT_INDEX, 0);

        } catch (IOException exception) {
            throw new ItemStreamException("Unable to scan directory: " + inputDirectory, exception);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(CURRENT_INDEX, currentIndex);
    }

    @Override
    public void close() {
        files = null;
    }

    @Override
    public BoardGameFile read() throws Exception {

        if (currentIndex >= files.size()) {
            return null;
        }

        Path file = files.get(currentIndex++);
        String gameId = file.getParent().getFileName().toString();

        return BoardGameFile.of(file, gameId, file.getFileName().toString());
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        return name.endsWith(".pdf")
                || name.endsWith(".docx")
                || name.endsWith(".txt");
    }
}
