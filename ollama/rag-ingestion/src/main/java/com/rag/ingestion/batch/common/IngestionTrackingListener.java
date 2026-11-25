package com.rag.ingestion.batch.common;

import com.rag.ingestion.domain.file.FileIngestionResult;
import com.rag.ingestion.domain.file.FileMetadata;
import com.rag.ingestion.domain.tracking.IngestedFileLogTrackingManager;

import org.jspecify.annotations.NullMarked;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.function.Function;

@NullMarked
public class IngestionTrackingListener<T>
        implements ItemProcessListener<T, FileIngestionResult>,
                ItemWriteListener<FileIngestionResult>,
                SkipListener<T, FileIngestionResult> {

    private final IngestedFileLogTrackingManager ingestedFileLogTrackingManager;
    private final Function<T, FileMetadata> metadataExtractor;
    private final String ingestionRequestId;

    public IngestionTrackingListener(
            IngestedFileLogTrackingManager ingestedFileLogTrackingManager,
            Function<T, FileMetadata> metadataExtractor,
            String ingestionRequestId) {
        this.ingestedFileLogTrackingManager = ingestedFileLogTrackingManager;
        this.metadataExtractor = metadataExtractor;
        this.ingestionRequestId = ingestionRequestId;
    }

    @Override
    public void beforeProcess(T item) {
        FileMetadata metadata = metadataExtractor.apply(item);
        ingestedFileLogTrackingManager.markProcessing(metadata, ingestionRequestId);
    }

    @Override
    public void afterWrite(Chunk<? extends FileIngestionResult> chunk) {
        chunk.getItems()
                .forEach(item ->
                        ingestedFileLogTrackingManager.markCompleted(
                                item.metadata(), item.documentCont()));
    }

    @Override
    public void onSkipInProcess(T item, Throwable throwable) {
        var fileMetaData = metadataExtractor.apply(item);
        ingestedFileLogTrackingManager.markFailedIfProcessing(fileMetaData, throwable);
    }

    @Override
    public void onSkipInWrite(FileIngestionResult fileIngestionResult, Throwable throwable) {
        ingestedFileLogTrackingManager.markFailedIfProcessing(
                fileIngestionResult.metadata(), throwable);
    }
}
