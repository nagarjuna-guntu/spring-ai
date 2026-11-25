package com.rag.hyde.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    @Bean("boardGameRagAdvisor")
    public Advisor boardGameRagAdvisor(@Qualifier("boardGameVectorStore") VectorStore vectorStore) {
        return QuestionAnswerAdvisor.builder(vectorStore).build();
    }

    @Bean("springDocRagAdvisor")
    public Advisor springDocRagAdvisor(
            @Qualifier("springDocVectorStore") VectorStore vectorStore,
            @Qualifier("hydeQueryTransformer") QueryTransformer hydeQueryTransformer) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(hydeQueryTransformer)
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .filterExpression(
                                        new FilterExpressionBuilder()
                                                .eq("documentType", "SPRING_BOOT_DOC")
                                                .build())
                                .vectorStore(vectorStore)
                                .build())
                .build();
    }

    @Bean("hydeQueryTransformer")
    QueryTransformer hydeQueryTransformer(ChatModel chatModel) {
        return HydeQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }
}
