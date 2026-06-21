package com.example.chatmemory.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AIConfig {

    /**
     * Creates the main ChatClient bean with configured advisors.
     *
     * @param chatClientBuilder Spring AI ChatClient builder
     * @param vectorStore configured vector store for RAG
     * @param chatMemory chat memory repository
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory) {

        return chatClientBuilder
                .defaultAdvisors(
                        simpleLoggerAdvisor(),
                        retrievalAugmentationAdvisor(chatClientBuilder, vectorStore),
                        messageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    /**
     * Creates a SimpleLoggerAdvisor for debugging advisor interactions.
     *
     * @return advisor for logging
     */
    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return SimpleLoggerAdvisor.builder()
                .order(Ordered.HIGHEST_PRECEDENCE)  // Run first
                .build();
    }

    /**
     * Creates ChatMemory for maintaining conversation history.
     *
     * @param chatMemoryRepository persistence layer for chat memory
     * @return configured chat memory instance
     */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(50) // default is 20
                .build();
    }

    /**
     * Creates the RetrievalAugmentationAdvisor for RAG-based retrieval.
     *
     * <p>This advisor retrieves relevant documents from the vector store
     * and augments the LLM prompt with them for more accurate responses.</p>
     *
     * @param chatClientBuilder builder for creating ChatClient for query transformers
     * @param vectorStore vector store for document retrieval
     * @return configured retrieval augmentation advisor
     */
    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever(vectorStore))
                .queryTransformers(
                        translationQueryTransformer(chatClientBuilder),
                        rewriteQueryTransformer(chatClientBuilder)
                )
                .queryExpander(multiQueryExpander(chatClientBuilder))
                .build();
    }



    /**
     * Creates a DocumentRetriever for vector store-based document retrieval.
     *
     * <p>This bean instantiates a VectorStoreDocumentRetriever that retrieves
     * semantically similar documents from the configured vector store. It serves as a
     * key component of the Retrieval Augmented Generation (RAG) pipeline, enabling
     * the LLM to fetch relevant contextual information when processing user queries.</p>
     * @param vectorStore vector store for document retrieval
     * @return configured document retriever
     */
    @Bean
    DocumentRetriever documentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .build();
    }

    /**
     * Creates a MultiQueryExpander to generate multiple variations of user queries.
     *
     * <p>Helps retrieve more relevant documents by expanding the query space.</p>
     *
     * @param chatClientBuilder builder for LLM integration
     * @return configured multi-query expander
     */
    @Bean
    QueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();
    }

    /**
     * Creates a MessageChatMemoryAdvisor for conversation history.
     *
     * @param chatMemory configured chat memory instance
     * @return advisor for managing memory
     */
    @Bean
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    /**
     * Creates a RewriteQueryTransformer to improve query quality.
     *
     * <p>Rewrites user queries to be more suitable for vector store retrieval.</p>
     *
     * @param chatClientBuilder builder for LLM integration
     * @return configured rewrite query transformer
     */
    @Bean
    RewriteQueryTransformer rewriteQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .build();

    }

    /**
     * Creates a TranslationQueryTransformer to normalize queries.
     *
     * <p>Translates queries to a standard language for consistent retrieval.</p>
     *
     * @param chatClientBuilder builder for LLM integration
     * @return configured translation query transformer
     */
    @Bean
    TranslationQueryTransformer translationQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate())
                .targetLanguage("English")
                .build();

    }
}
