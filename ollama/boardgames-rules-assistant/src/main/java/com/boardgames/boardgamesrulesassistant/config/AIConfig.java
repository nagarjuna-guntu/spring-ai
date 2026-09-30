package com.boardgames.boardgamesrulesassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    /**
     * 1. The primary ChatClient bean.
     * Spring automatically applies all ChatClientBuilderCustomizer beans registered below
     * to the 'chatClientBuilder' before it arrives here.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addChatMemory(VectorStoreChatMemoryAdvisor chatMemoryAdvisor) {
        return builder -> builder.defaultAdvisors(chatMemoryAdvisor);
    }

    @Bean
    ChatClientBuilderCustomizer addRagAdvisor(RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return builder -> builder.defaultAdvisors(retrievalAugmentationAdvisor);
    }

    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder.defaultAdvisors(SimpleLoggerAdvisor.builder().build());
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .build();
    }

    @Bean
    VectorStoreChatMemoryAdvisor chatMemoryAdvisor(VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore).build();
    }

    @Bean
    VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .build();
    }
}