package com.rag.ingestion.domain.tracking;

import com.rag.ingestion.domain.common.IngestionDomain;
import com.rag.ingestion.domain.common.IngestionStatus;
import com.rag.ingestion.domain.file.FileMetadata;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngestedFileLogRepository extends ListCrudRepository<IngestedFileLog, Long> {

    Optional<IngestedFileLog> findByIngestionDomainAndFilePath(
            IngestionDomain ingestionDomain, String filePath);

    boolean existsByIngestionDomainAndFilePathAndLastModifiedTimeAndFileSizeAndIngestionStatus(
            IngestionDomain ingestionDomain,
            String filePath,
            Instant lastModifiedTime,
            long fileSize,
            IngestionStatus ingestionStatus);

    default boolean existsUnchangedWithStatus(FileMetadata metadata, IngestionStatus status) {
        return existsByIngestionDomainAndFilePathAndLastModifiedTimeAndFileSizeAndIngestionStatus(
                metadata.domain(),
                metadata.filePath(),
                metadata.lastModifiedTime(),
                metadata.fileSize(),
                status);
    }

    List<IngestedFileLog> findAllByIngestionDomain(IngestionDomain ingestionDomain);

    List<IngestedFileLog> findAllByIngestionDomainAndIngestionStatus(
            IngestionDomain ingestionDomain, IngestionStatus ingestionStatus);

    @Modifying
    @Query(
            """
            UPDATE ingested_file_log
                SET status = 'FAILED',
                    last_error = :errorMessage,
                    updated_at = CURRENT_TIMESTAMP,
                    version = version + 1
                WHERE ingestion_request_id = :ingestionRequestId
                  AND status = 'PROCESSING'
            """)
    int markProcessingFilesFailed(
            @Param("ingestionRequestId") String ingestionRequestId,
            @Param("errorMessage") String errorMessage);
}
