package com.rag.ingestion.config.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@Configuration
@EnableJdbcAuditing // used for creation/update dates for jdbc table columns
public class JdbcAuditingConfig {}
