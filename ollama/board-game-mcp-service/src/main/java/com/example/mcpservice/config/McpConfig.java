package com.example.mcpservice.config;

import com.example.mcpservice.domain.GameTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    ToolCallbackProvider toolCallbackProvider(GameTools gameTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(gameTools)
                .build();
    }
}
