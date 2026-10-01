package com.example.toolsearchtool.config;

import com.example.toolsearchtool.tools.DummyTools;
import com.example.toolsearchtool.tools.MyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class AIConfig {

    private static final String question = """
            Help me plan what to wear today in Visakhapatnam.
            Please suggest clothing shops that are open right now.
            """;

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    ChatClientBuilderCustomizer addLogger() {
        return builder -> builder
                .defaultAdvisors(SimpleLoggerAdvisor.builder()
                        .order(Ordered.HIGHEST_PRECEDENCE + 2000)
                        .build()
                );
    }

    @Bean
    ChatClientBuilderCustomizer addToolSearchAdvisor(VectorStore vectorStore) {
        return builder -> builder
                .defaultAdvisors(ToolSearchToolCallingAdvisor.builder()
                        .toolIndex(new VectorToolIndex(vectorStore))
                        .maxResults(4)
                        .build()
                );
    }

    @Bean
    ChatClientBuilderCustomizer addTools() {
        return builder -> builder
                .defaultTools(new DummyTools(), new MyTools()
                );
    }

    @Bean
    CommandLineRunner initiate(ChatClient chatClient) {
        return args -> {
            IO.println("LLM Answering Question...");
            // Add any additional initialization logic here
            var answer = chatClient.prompt(question)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "my-session-id"))
                    .call()
                    .content();
            IO.println("LLM Answer: " + answer);

        };
    }
}
