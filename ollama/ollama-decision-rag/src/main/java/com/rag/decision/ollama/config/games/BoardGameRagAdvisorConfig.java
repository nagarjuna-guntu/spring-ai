package com.rag.decision.ollama.config.games;

import com.rag.decision.ollama.config.HybridDocumentRetriever;
import com.rag.decision.ollama.config.ReciprocalRankFusionPreservingDocumentJoiner;
import com.rag.decision.ollama.config.RerankingDocumentPostProcessor;
import com.rag.decision.ollama.config.lucene.LuceneFilterQueryMapper;
import com.rag.decision.ollama.domain.LuceneSearchService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.question.YesNoQuestion;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.postretrieval.document.DecisionDocumentFilter;
import org.springframework.ai.rag.postretrieval.document.DecisionDocumentReranker;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;

@Configuration
public class BoardGameRagAdvisorConfig {

    @Bean("boardGameRagAdvisor")
    public Advisor boardGameRagAdvisor(
            @Qualifier("boardGameHydeQueryTransformer")
                    QueryTransformer boardGameHydeQueryTransformer,
            @Qualifier("boardGameHybridRetriever") DocumentRetriever boardGameHybridRetriever,
            @Qualifier("boardGameDecisionFilter") DocumentPostProcessor boardGameDecisionFilter,
            @Qualifier("boardGameDecisionReranker") DocumentPostProcessor boardGameDecisionReranker,
            @Qualifier("boardGameLlmReranker") DocumentPostProcessor boardGameLlmReranker,
            ReciprocalRankFusionPreservingDocumentJoiner documentJoiner) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(boardGameHydeQueryTransformer)
                .documentRetriever(boardGameHybridRetriever)
                .documentPostProcessors(
                        boardGameDecisionFilter, boardGameDecisionReranker, boardGameLlmReranker)
                .documentJoiner(documentJoiner)
                .build();
    }

    @Bean("boardGameHydeQueryTransformer")
    QueryTransformer boardGameHydeQueryTransformer(ChatModel chatModel) {
        return BoardGameHydeQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean("boardGameHybridRetriever")
    public DocumentRetriever boardGameHybridRetriever(
            @Qualifier("boardGameVectorStore") VectorStore boardGameVectorStore,
            @Qualifier("boardGameLuceneSearch") LuceneSearchService boardGameLuceneSearch,
            LuceneFilterQueryMapper filterQueryMapper) {

        return new HybridDocumentRetriever(
                boardGameVectorStore, boardGameLuceneSearch, filterQueryMapper, 8);
    }

    @Bean("boardGameDecisionFilter")
    public DocumentPostProcessor boardGameDecisionFilter(DecisionModel decisionModel) {
        return DecisionDocumentFilter.builder(decisionModel)
                .policy(new DecisionDocumentFilter.Policy(0.85, 0.85, 0.45, 0.55))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean("boardGameDecisionReranker")
    public DocumentPostProcessor boardGameDecisionReranker(DecisionModel decisionModel) {
        return DecisionDocumentReranker.builder(decisionModel)
                .topK(6)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .timeout(Duration.ofMinutes(2))
                .question(
                        YesNoQuestion.builder()
                                .text(
                                        """
                                        Does the `passage` state game rules, setup,
                                        scoring, or turn actions relevant to the `query`?\
                                        """)
                                .whenYes(
                                        """
                                        The passage defines rules, mechanics,
                                        or constraints needed to answer the board game question\
                                        """)
                                .whenNo(
                                        """
                                        The passage is flavor text, unrelated components,
                                        or rules for a different phase\
                                        """)
                                .build())
                .build();
    }

    @Bean("boardGameLlmReranker")
    public DocumentPostProcessor boardGameLlmReranker(ChatModel chatModel) {
        return RerankingDocumentPostProcessor.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .topK(3)
                .build();
    }
}
