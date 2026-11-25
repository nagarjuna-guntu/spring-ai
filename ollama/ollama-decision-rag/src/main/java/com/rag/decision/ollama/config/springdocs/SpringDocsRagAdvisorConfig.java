package com.rag.decision.ollama.config.springdocs;

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
public class SpringDocsRagAdvisorConfig {

    @Bean("springDocRagAdvisor")
    public Advisor springDocRagAdvisor(
            @Qualifier("springDocHydeQueryTransformer")
                    QueryTransformer springDocHydeQueryTransformer,
            @Qualifier("springDocsHybridRetriever") DocumentRetriever springDocsHybridRetriever,
            @Qualifier("springDocsDecisionFilter") DocumentPostProcessor springDocsDecisionFilter,
            @Qualifier("springDocsDecisionReranker")
                    DocumentPostProcessor springDocsDecisionReranker,
            @Qualifier("springDocsLlmReranker") DocumentPostProcessor springDocsLlmReranker,
            ReciprocalRankFusionPreservingDocumentJoiner documentJoiner) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(springDocHydeQueryTransformer)
                .documentRetriever(springDocsHybridRetriever)
                .documentPostProcessors(
                        springDocsDecisionFilter, springDocsDecisionReranker, springDocsLlmReranker)
                .documentJoiner(documentJoiner)
                .build();
    }

    @Bean("springDocHydeQueryTransformer")
    QueryTransformer springDocHydeQueryTransformer(ChatModel chatModel) {
        return SpringDocHydeQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .build();
    }

    @Bean("springDocsHybridRetriever")
    public DocumentRetriever springDocsHybridRetriever(
            @Qualifier("springDocsVectorStore") VectorStore springDocsVectorStore,
            @Qualifier("springDocsLuceneSearch") LuceneSearchService springDocsLuceneSearch,
            LuceneFilterQueryMapper filterQueryMapper) {

        return new HybridDocumentRetriever(
                springDocsVectorStore, springDocsLuceneSearch, filterQueryMapper, 8);
    }

    @Bean("springDocsDecisionFilter")
    public DocumentPostProcessor springDocsDecisionFilter(DecisionModel decisionModel) {
        return DecisionDocumentFilter.builder(decisionModel)
                .policy(new DecisionDocumentFilter.Policy(0.75, 0.75, 0.45, 0.55))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .timeout(Duration.ofMinutes(2))
                .build();
    }

    @Bean("springDocsDecisionReranker")
    public DocumentPostProcessor springDocsDecisionReranker(DecisionModel decisionModel) {
        return DecisionDocumentReranker.builder(decisionModel)
                .topK(6)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .timeout(Duration.ofMinutes(2))
                .question(
                        YesNoQuestion.builder()
                                .text(
                                        """
                                        Does the `passage` explain the Spring Boot API, configuration property,
                                        observability, fault tolerance or behavior asked in the `query`?
                                        """)
                                .whenYes(
                                        """
                                        The passage documents the exact class, annotation, property, or runtime behaviour,
                                        configurations,observability, fault tolerance
                                        """)
                                .whenNo(
                                        """
                                        The passage covers an unrelated Spring module or generic boilerplate
                                        """)
                                .build())
                .build();
    }

    @Bean("springDocsLlmReranker")
    public DocumentPostProcessor springDocsLlmReranker(ChatModel chatModel) {
        return RerankingDocumentPostProcessor.builder()
                .chatClientBuilder(ChatClient.builder(chatModel))
                .topK(4)
                .build();
    }
}
