package com.example.tools.config;

import com.example.tools.domain.game.GameComplexityRequest;
import com.example.tools.domain.game.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class GameTools {

    @Bean
    ToolCallback gameComplexityByTitle(GameService gameService) {
        return FunctionToolCallback.builder("gameComplexityByTitle", gameService::getGameComplexity)
                .description("""
                        Use this tool to calculate the technical and strategic complexity score
                        of a board game or video game based on its title.
                        """)
                .inputType(GameComplexityRequest.class)
                .build();

    }

}
