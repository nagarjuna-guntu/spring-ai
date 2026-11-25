package com.rag.rerank.huggingface;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RerankRagHuggingfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RerankRagHuggingfaceApplication.class, args);
    }
}
