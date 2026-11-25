package com.rag.decision.ollama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OllamaDecisionRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(OllamaDecisionRagApplication.class, args);
    }
}
