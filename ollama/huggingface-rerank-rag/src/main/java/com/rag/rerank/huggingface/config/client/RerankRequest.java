package com.rag.rerank.huggingface.config.client;

import java.util.List;

public record RerankRequest(String query, List<String> texts, boolean truncate) {
    
    public static RerankRequest of(String query, List<String> texts) {
        return new RerankRequest(query, texts, true);
    }
    public static RerankRequest of(String query, List<String> texts, boolean truncate) {
        return new RerankRequest(query, texts, truncate);
    }
}
