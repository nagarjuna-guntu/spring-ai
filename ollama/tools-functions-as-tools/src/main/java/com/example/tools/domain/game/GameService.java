package com.example.tools.domain.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public GameComplexityResponse getGameComplexity(GameComplexityRequest gameComplexityRequest) {
        var gameSlug = gameComplexityRequest.title().toLowerCase().replace(" ", "_");
        log.info("getting the complexity for the game - {} ",  gameSlug);
        return gameRepository.findBySlug(gameSlug)
                .map(game ->
                        GameComplexityResponse.of(game.title(), game.complexityEnum()))
                .orElseGet(() -> {
                    log.warn("Game not found: {}", gameSlug);
                    return GameComplexityResponse.of(Game.UNKOWN_GAME.title(), Game.UNKOWN_GAME.complexityEnum());
                });
    }
}
