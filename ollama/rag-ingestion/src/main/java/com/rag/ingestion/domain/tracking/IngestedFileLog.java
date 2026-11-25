package com.rag.ingestion.domain.tracking;

import com.rag.ingestion.domain.common.IngestionDomain;
import com.rag.ingestion.domain.common.IngestionStatus;

import lombok.Builder;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("ingested_file_log")
@Builder(toBuilder = true)
public record IngestedFileLog(
        @Id Long id,
        @Column("domain") IngestionDomain ingestionDomain,
        @Column("file_path") String filePath,
        @Column("last_modified_time") Instant lastModifiedTime,
        @Column("file_size") long fileSize,
        @Column("status") IngestionStatus ingestionStatus,
        @Column("document_count") Integer documentCount,
        @Column("attempt_count") int attemptCount,
        @Column("last_ingested_at") Instant lastIngestedAt,
        @Column("last_error") String lastError,
        @CreatedDate @Column("created_at") Instant createdAt,
        @LastModifiedDate @Column("updated_at") Instant updatedAt,
        @Column("ingestion_request_id") String ingestionRequestId,
        @Version @Column("version") int version) {}
