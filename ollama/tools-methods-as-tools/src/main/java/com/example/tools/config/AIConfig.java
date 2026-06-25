package com.example.tools.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore
    ) {

        return chatClientBuilder
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        vectorStoreChatMemoryAdvisor(vectorStore),
                        retrievalAugmentationAdvisor(chatClientBuilder, vectorStore)
                )
                .build();
    }

    @Bean
    VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(
            VectorStore vectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                .build();
    }

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(ChatClient.Builder chatClientBuilder,
                                                              VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .build()
                )
                .queryTransformers(
                        translationQueryTransformer(chatClientBuilder),
                        rewriteQueryTransformer(chatClientBuilder)
                )
                .queryExpander(multiQueryExpander(chatClientBuilder))
                .build();
    }

    @Bean
    QueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
    }

    @Bean
    RewriteQueryTransformer rewriteQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .build();

    }

    @Bean
    TranslationQueryTransformer translationQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .targetLanguage("English")
                .build();

    }
}
