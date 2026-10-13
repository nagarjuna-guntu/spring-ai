package com.boardgamerules.modularrag.config;

import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.rag.JevDocumentFilter;
import org.springaicommunity.typesafe.rag.JevDocumentReranker;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

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
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStoreDocumentRetriever vectorStoreDocumentRetriever,
                                                                     RewriteQueryTransformer rewriteQueryTransformer,
                                                                     MultiQueryExpander multiQueryExpander,
                                                                     TypeSafeClient typeSafeClient,
                                                                     TaskExecutor taskExecutor) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(rewriteQueryTransformer)
                .queryExpander(multiQueryExpander)
                .documentRetriever(vectorStoreDocumentRetriever)
                .documentPostProcessors(
                        JevDocumentFilter.builder(typeSafeClient).build(),
                        JevDocumentReranker.builder(typeSafeClient).topK(2).build())
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                .allowEmptyContext(true).build())
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    VectorStoreChatMemoryAdvisor chatMemoryAdvisor(@Qualifier("chatMemoryVectorStore") VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore).build();
    }

    @Bean
    RewriteQueryTransformer rewriteQueryTransformer(ChatModel chatModel) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean
    MultiQueryExpander multiQueryExpander(ChatModel chatModel) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean
    VectorStoreDocumentRetriever vectorStoreDocumentRetriever(@Qualifier("gameRulesVectorStore") VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(4)
                .build();
    }
}