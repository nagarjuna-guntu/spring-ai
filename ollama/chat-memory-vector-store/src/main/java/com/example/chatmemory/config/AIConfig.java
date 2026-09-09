package com.example.chatmemory.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

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

    /**
     * 2. REGISTER A CUSTOMIZER
     * Automatically registers your global core advisors (Logging, Memory, and RAG)
     * to the autoconfigured framework builder.
     */
    @Bean
    ChatClientBuilderCustomizer advisorConfiguringCustomizer(VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor,
                                                             RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return builder -> builder.defaultAdvisors(
                new SimpleLoggerAdvisor(),
                vectorStoreChatMemoryAdvisor,
                retrievalAugmentationAdvisor);
    }

    @Bean
    VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .order(Ordered.HIGHEST_PRECEDENCE)
                .build();
    }

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever,
                                                              TranslationQueryTransformer translationQueryTransformer,
                                                              RewriteQueryTransformer rewriteQueryTransformer,
                                                              MultiQueryExpander multiQueryExpander) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(vectorStoreDocumentRetriever)
                .queryTransformers(translationQueryTransformer, rewriteQueryTransformer)
                .queryExpander(multiQueryExpander)
                .build();
    }

    @Bean
    VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .build();
    }

    /*
     * NOTE: Instead of injecting ChatClient.Builder into the subcomponents
     * (which would cause infinite loops), we inject the root 'ChatModel' bean
     * to create clean standalone sub-builders for internal RAG pipelines.
     */
    @Bean
    MultiQueryExpander multiQueryExpander(ChatModel chatModel) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .numberOfQueries(2)
                .includeOriginal(true)
                .build();
    }

    @Bean
    RewriteQueryTransformer rewriteQueryTransformer(ChatModel chatModel) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean
    TranslationQueryTransformer translationQueryTransformer(ChatModel chatModel) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .targetLanguage("English")
                .build();
    }
}
