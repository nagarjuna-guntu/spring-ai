package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class FileMover {

    private final DocumentPaths documentPaths;

    public FileMover(DocumentPaths documentPaths) {
        this.documentPaths = documentPaths;
    }

    public void moveProcessedFile(String filePath) {
        moveFile(filePath, documentPaths.processed());
    }

    public void moveToDLQ(File file) {
        moveFile(file.getAbsolutePath(), documentPaths.dlq());
    }

    private void moveFile(String filePath, String targetDirPath) {
        try {
            Path source = Path.of(filePath);
            if (!Files.exists(source)) {
                log.warn("File not found for move: {}", filePath);
                return;
            }
            Path targetDir = Path.of(targetDirPath);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(source.getFileName());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved file to {}", target);
        } catch (Exception e) {
            log.error("File move failed: {}", filePath, e);
        }
    }
}

