package com.rag.rerank.huggingface.config;

import com.rag.rerank.huggingface.config.client.TeiRerankClient;
import com.rag.rerank.huggingface.config.lucene.LuceneFilterQueryMapper;
import com.rag.rerank.huggingface.domain.LuceneSearchService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    @Bean("boardGameRagAdvisor")
    public Advisor boardGameRagAdvisor(
            @Qualifier("boardGameHydeQueryTransformer")
                    QueryTransformer boardGameHydeQueryTransformer,
            @Qualifier("boardGameHybridRetriever") DocumentRetriever boardGameHybridRetriever,
            @Qualifier("boardGameTeiReranker") DocumentPostProcessor boardGameTeiReranker,
            @Qualifier("boardGameReranker") DocumentPostProcessor boardGameReranker,
            ReciprocalRankFusionPreservingDocumentJoiner documentJoiner) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(boardGameHydeQueryTransformer)
                .documentRetriever(boardGameHybridRetriever)
                .documentPostProcessors(boardGameTeiReranker, boardGameReranker)
                .documentJoiner(documentJoiner)
                .build();
    }

    @Bean("springDocRagAdvisor")
    public Advisor springDocRagAdvisor(
            @Qualifier("springDocHydeQueryTransformer")
                    QueryTransformer springDocHydeQueryTransformer,
            @Qualifier("springDocsHybridRetriever") DocumentRetriever springDocsHybridRetriever,
            @Qualifier("springDocsTeiReranker") DocumentPostProcessor springDocsTeiReranker,
            @Qualifier("springDocsReranker") DocumentPostProcessor springDocsReranker,
            ReciprocalRankFusionPreservingDocumentJoiner documentJoiner) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(springDocHydeQueryTransformer)
                .documentRetriever(springDocsHybridRetriever)
                .documentPostProcessors(springDocsTeiReranker, springDocsReranker)
                .documentJoiner(documentJoiner)
                .build();
    }

    @Bean("boardGameHydeQueryTransformer")
    QueryTransformer boardGameHydeQueryTransformer(ChatModel chatModel) {
        return BoardGameHydeQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean("springDocHydeQueryTransformer")
    QueryTransformer springDocHydeQueryTransformer(ChatModel chatModel) {
        return SpringDocHydeQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean("boardGameHybridRetriever")
    public DocumentRetriever boardGameHybridRetriever(
            @Qualifier("boardGameVectorStore") VectorStore boardGameVectorStore,
            @Qualifier("boardGameLuceneSearch") LuceneSearchService boardGameLuceneSearch,
            LuceneFilterQueryMapper filterQueryMapper) {

        return new HybridDocumentRetriever(
                boardGameVectorStore, boardGameLuceneSearch, filterQueryMapper, 4);
    }

    @Bean("springDocsHybridRetriever")
    public DocumentRetriever springDocsHybridRetriever(
            @Qualifier("springDocsVectorStore") VectorStore springDocsVectorStore,
            @Qualifier("springDocsLuceneSearch") LuceneSearchService springDocsLuceneSearch,
            LuceneFilterQueryMapper filterQueryMapper) {

        return new HybridDocumentRetriever(
                springDocsVectorStore, springDocsLuceneSearch, filterQueryMapper, 5);
    }

    @Bean("boardGameReranker")
    public DocumentPostProcessor boardGameReranker(ChatModel chatModel) {
        return RerankingDocumentPostProcessor.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .topK(2)
                .build();
    }

    @Bean("springDocsReranker")
    public DocumentPostProcessor springDocsReranker(ChatModel chatModel) {
        return RerankingDocumentPostProcessor.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .topK(2)
                .build();
    }

    @Bean("boardGameTeiReranker")
    public DocumentPostProcessor boardGameTeiReranker(TeiRerankClient teiRerankClient) {
        return TeiRerankingDocumentPostProcessor.builder()
                .rerankClient(teiRerankClient)
                .topK(3)
                .build();
    }

    @Bean("springDocsTeiReranker")
    public DocumentPostProcessor springDocsTeiReranker(TeiRerankClient teiRerankClient) {
        return TeiRerankingDocumentPostProcessor.builder()
                .rerankClient(teiRerankClient)
                .topK(3)
                .build();
    }
}
