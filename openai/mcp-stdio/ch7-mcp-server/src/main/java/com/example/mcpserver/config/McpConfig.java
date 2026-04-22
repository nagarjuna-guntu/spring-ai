package com.example.mcpserver.config;

import com.example.mcpserver.domain.GameTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    ToolCallbackProvider toolCallbackProvider(GameTools gameTools) {
        return ToolCallbackProvider.from(ToolCallbacks.from(gameTools));
    }
}


