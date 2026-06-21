package com.example.chatmemoryredis.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

@Configuration
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
public class AIConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory) {

        return chatClientBuilder
                .defaultAdvisors(
                        retrievalAugmentationAdvisor(chatClientBuilder, vectorStore),
                        messageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    // 1. Instantiates Jedis client cleanly using configuration records
    @Bean
    public RedisClient jedisRedisClient(RedisChatMemoryProperties properties) {
        return RedisClient.builder()
                .hostAndPort(properties.host(), properties.port())
                .build();
    }

    // 2. Builds your repository cleanly using type-safe getters
    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(RedisClient jedisClient, RedisChatMemoryProperties properties) {
        return RedisChatMemoryRepository.builder()
                .jedisClient(jedisClient)
                .indexName(properties.indexName())
                .keyPrefix(properties.keyPrefix())
                .timeToLive(properties.timeToLive())
                .build();
    }

    @Bean
    ChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(10) // default is 20
                .build();
    }

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
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
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
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
