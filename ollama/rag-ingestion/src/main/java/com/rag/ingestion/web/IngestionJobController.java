package com.rag.ingestion.web;

import com.rag.ingestion.domain.common.IngestionJobService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion/jobs")
public class IngestionJobController {
    private final IngestionJobService ingestionJobService;

    public IngestionJobController(IngestionJobService ingestionJobService) {
        this.ingestionJobService = ingestionJobService;
    }

    @PostMapping
    public IngestionJobResponse startIngestionJob(@RequestBody IngestionJobRequest request)
            throws Exception {
        return ingestionJobService.start(request);
    }
}
