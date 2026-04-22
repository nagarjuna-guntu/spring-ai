package com.example.boardgamebuddy.config;

import com.example.boardgamebuddy.gamedata.GameComplexityRequest;
import com.example.boardgamebuddy.gamedata.GameTools;
import com.example.boardgamebuddy.weather.WeatherRequest;
import com.example.boardgamebuddy.weather.WeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolsFunctionsConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        ToolCallback toolCallback = FunctionToolCallback
                .builder("gameTools", new WeatherService())
                .description("Get the weather in location")
                .inputType(WeatherRequest.class)
                .build();
        return chatClientBuilder
                .defaultToolCallbacks(toolCallback)
                .build();
    }
}
