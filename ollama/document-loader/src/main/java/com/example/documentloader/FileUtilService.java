package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.integration.file.filters.FileSystemPersistentAcceptOnceFileListFilter;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Slf4j
public class FileUtilService {

    private final FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter;

    public FileUtilService(FileSystemPersistentAcceptOnceFileListFilter persistentFileFilter) {
        this.persistentFileFilter = persistentFileFilter;
    }

    public File getFile(Message<byte[]> message) {
        Object fileObj = message.getHeaders().get("file_originalFile");
        if (fileObj instanceof File file && file.exists()) {
            return file;
        }
        throw new IllegalArgumentException("Invalid file reference");
    }

    public String findFilePath(List<Document> documents) {
        return documents.stream()
                .findFirst()
                .map(doc -> doc.getMetadata().get("file_originalFile_path"))
                .map(Object::toString)
                .orElse("unknown");
    }

    public boolean isPremiumDocument(File file) {
        var fileName = file.toPath().getFileName().toString();
        log.info("[{}] Checking if file contains premium tag.", fileName);
        //Get the base file name without extension and check for premium tags
        var baseFileName = StringUtils.stripFilenameExtension(fileName);
        return StringUtils.endsWithIgnoreCase(baseFileName, "-premium") ||
                StringUtils.endsWithIgnoreCase(baseFileName, "_premium") ||
                StringUtils.endsWithIgnoreCase(baseFileName, " premium");
    }

    /**
     * Rolls back file acceptance after a processing failure so the file
     * can be accepted again without restarting the application.
     */
    public void rollbackFileAcceptance(String absoluteFilePath) {
        if (absoluteFilePath == null || absoluteFilePath.isBlank()) {
            log.warn("roll back file acceptance for non-existent file: [{}] - Failed.", absoluteFilePath);
            return;
        }
        try {
            var filePath = Path.of(absoluteFilePath);
            if (!Files.exists(filePath)) {
                log.warn("roll back file acceptance for non-existent file: {}.", absoluteFilePath);
                return;
            }
            boolean removed = persistentFileFilter.remove(filePath.toFile());
            log.info("Rolled back file acceptance. file=[{}], removed=[{}]", absoluteFilePath, removed);
        } catch (Exception ex) {
            log.error("Failed to roll back file acceptance for file: {}", absoluteFilePath, ex);
        }
    }
}
