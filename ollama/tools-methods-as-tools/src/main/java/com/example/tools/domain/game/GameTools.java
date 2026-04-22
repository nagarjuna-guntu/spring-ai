package com.example.tools.domain.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameTools {

    private final GameRepository gameRepository;

    public GameTools(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Tool(name = "gameComplexityByTitle",
            description = """
                    Use this tool to calculate the technical and strategic complexity score 
                    of a board game or video game based on its title.
                    """
    )
    public GameComplexityResponse gameComplexityByTitle(
            @ToolParam(description = "Game title") String gameTitle) {
        var gameSlug = gameTitle.toLowerCase().replace(" ", "_");
        log.info("getting the complexity for the game - {} - {}", gameTitle, gameSlug);
        return gameRepository.findBySlug(gameSlug)
                .map(game ->
                        GameComplexityResponse.of(game.title(), game.complexityEnum()))
                .orElseGet(() -> {
                    log.warn("Game not found: {}", gameSlug);
                    return GameComplexityResponse.of(Game.UNKOWN_GAME.title(), Game.UNKOWN_GAME.complexityEnum());
                });
    }
}
