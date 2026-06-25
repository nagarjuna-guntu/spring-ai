package com.example.summarizingcontent.config;

import com.example.summarizingcontent.domain.Answer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {


        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                //.similarityThreshold(0.78)
                //.topK(8)
                .build();

        var outputValidationAdvisor = StructuredOutputValidationAdvisor
                .builder()
                .maxRepeatAttempts(2)
                .outputType(Answer.class)
                .build();

        var advisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
        return chatClientBuilder
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build(), outputValidationAdvisor, advisor)
                .build();
    }
}
