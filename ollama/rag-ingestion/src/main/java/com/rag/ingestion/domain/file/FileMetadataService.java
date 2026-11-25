package com.rag.ingestion.domain.file;

import com.rag.ingestion.domain.common.IngestionDomain;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@NullMarked
public class FileMetadataService {

    public FileMetadata getMetadata(
            IngestionDomain ingestionDomain, Path rootDirectory, Path file) {

        Assert.notNull(ingestionDomain, "Ingestion domain must not be null");
        Assert.notNull(rootDirectory, "Root directory must not be null");
        Assert.notNull(file, "File path must not be null");

        Path normalizedRoot = rootDirectory.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();

        Assert.isTrue(
                normalizedFile.startsWith(normalizedRoot),
                () ->
                        "File [%s] is outside the configured root directory [%s]"
                                .formatted(normalizedFile, normalizedRoot));
        try {
            BasicFileAttributes fileAttributes = readFileAttributes(normalizedFile);
            String relativePath = toRelativePath(normalizedRoot, normalizedFile);
            Instant lastModifiedTimeAt =
                    fileAttributes.lastModifiedTime().toInstant().truncatedTo(ChronoUnit.MICROS);

            return FileMetadata.of(
                    ingestionDomain,
                    relativePath,
                    lastModifiedTimeAt,
                    fileAttributes.size(),
                    normalizedFile);

        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read file metadata: " + file, exception);
        }
    }

    private BasicFileAttributes readFileAttributes(Path normalizedFile) throws IOException {
        BasicFileAttributes fileAttributes =
                Files.readAttributes(normalizedFile, BasicFileAttributes.class);
        Assert.isTrue(
                fileAttributes.isRegularFile(),
                () -> "[%s] is not a regular file.".formatted(normalizedFile));

        return fileAttributes;
    }

    private String toRelativePath(Path normalizedRoot, Path normalizedFile) {
        return normalizedRoot.relativize(normalizedFile).toString().replace("\\", "/");
    }
}
