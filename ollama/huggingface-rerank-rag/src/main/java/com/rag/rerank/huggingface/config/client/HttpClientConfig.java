package com.rag.rerank.huggingface.config.client;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration
@ImportHttpServices(group = "tei-reranker", types = TeiRerankClient.class)
public class HttpClientConfig {}
