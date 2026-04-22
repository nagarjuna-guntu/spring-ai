package com.example.tools.config;

import com.example.tools.domain.game.GameComplexityRequest;
import com.example.tools.domain.game.GameComplexityResponse;
import com.example.tools.domain.game.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class FunctionToolsConfig {
    public static final String GAME_COMPLEXITY = "gameComplexityByTitle";

    @Bean(GAME_COMPLEXITY)
    @Description("""
                 Use this tool to calculate the technical and strategic complexity score
                 of a board game or video game based on its title.
                 """)
    Function<GameComplexityRequest, GameComplexityResponse> gameComplexityByTitle(GameService gameService) {
        return gameService::getGameComplexity;
    }
}
