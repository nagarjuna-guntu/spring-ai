package com.example.boardgamebuddy.config;

import com.example.boardgamebuddy.gamedata.GameTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, GameTools gameTools) {

        var vectorStoreChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore).build();
        //var chatAdvisor2 = PromptChatMemoryAdvisor.builder(chatMemory).build();
        // Both advisors use MessageWindowChatMemory as implementation of ChatMemory
        // uses ChatMemoryRepository, which uses InMemoryChatMemoryRepository
        // as default implementation, which uses MAP (ConcurrentHashMap) as a memory store

        var ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .build())
                .queryTransformers(
                        TranslationQueryTransformer.builder()
                                .targetLanguage("English")
                                .chatClientBuilder(chatClientBuilder)
                                .build(),
                        RewriteQueryTransformer.builder()
                                .chatClientBuilder(chatClientBuilder)
                                .build()
                )
                .build();
        return chatClientBuilder
                .defaultAdvisors(ragAdvisor, vectorStoreChatMemoryAdvisor)
                .defaultToolNames("gameTools")
                .build();

    }
}
