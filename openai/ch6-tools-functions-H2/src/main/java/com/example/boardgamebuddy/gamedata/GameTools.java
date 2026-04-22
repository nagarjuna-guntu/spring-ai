package com.example.boardgamebuddy.gamedata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("gameTools")
@Description("Fetches the complexity of a game.")
@Slf4j
public class GameTools implements Function<GameComplexityRequest, GameComplexityResponse> {
    private final GameRepository gameRepository;

    public GameTools(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Tool(name = "getGameComplexity",
            description = """
                    Returns a game's complexity/difficulty given
                    by the game's tittle/name.
                    """
    )
    public GameComplexityResponse apply(GameComplexityRequest gameComplexityRequest) {

        var gameSlug = gameComplexityRequest.title().toLowerCase().replace(" ", "_");

        log.info("Getting complexity for {} ({})", gameComplexityRequest.title(), gameSlug);

        return gameRepository.findBySlug(gameSlug)
                .map(game -> GameComplexityResponse.of(game.title(), game.gameComplexity()))
                .orElseGet(() -> {
                    log.warn("Game not found: {}", gameSlug);
                    return GameComplexityResponse.of(Game.UNKOWN_GAME.title(), Game.UNKOWN_GAME.gameComplexity());
                });


    }
}
