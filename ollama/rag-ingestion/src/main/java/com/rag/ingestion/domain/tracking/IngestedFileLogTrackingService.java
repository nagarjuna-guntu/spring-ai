package com.rag.ingestion.domain.tracking;

import com.rag.ingestion.domain.common.IngestionStatus;
import com.rag.ingestion.domain.file.FileMetadata;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Slf4j
@Transactional(readOnly = true)
public class IngestedFileLogTrackingService {

    private final IngestedFileLogRepository ingestedFileLogRepository;

    public IngestedFileLogTrackingService(IngestedFileLogRepository ingestedFileLogRepository) {
        this.ingestedFileLogRepository = ingestedFileLogRepository;
    }

    public boolean isAlreadyIngested(FileMetadata metadata) {
        Assert.notNull(metadata, "File metadata must not be null");

        boolean alreadyIngested =
                ingestedFileLogRepository.existsUnchangedWithStatus(
                        metadata, IngestionStatus.COMPLETED);
        log.atDebug()
                .addKeyValue("domain", metadata.domain())
                .addKeyValue("filePath", metadata.filePath())
                .addKeyValue("alreadyIngested", alreadyIngested)
                .log("Checked file ingestion status.");

        return alreadyIngested;
    }

    public boolean needsIngestion(FileMetadata metadata) {
        return !isAlreadyIngested(metadata);
    }
}
