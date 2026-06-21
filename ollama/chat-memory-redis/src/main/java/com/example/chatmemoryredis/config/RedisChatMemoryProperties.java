package com.example.chatmemoryredis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "spring.ai.chat.memory.redis")
public record RedisChatMemoryProperties(
        @DefaultValue("localhost") String host,
        @DefaultValue("6379") int port,
        @DefaultValue("chat-memory-idx") String indexName,
        @DefaultValue("chat-memory:") String keyPrefix,
        @DefaultValue("24h") Duration timeToLive,
        @DefaultValue("true") boolean initializeSchema
) {
}
