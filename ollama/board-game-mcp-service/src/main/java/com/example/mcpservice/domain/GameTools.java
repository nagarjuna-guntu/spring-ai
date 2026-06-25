package com.example.mcpservice.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class GameTools {
    private final GameRepository gameRepository;


    public GameTools(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @McpTool(name = "gameCount",
            description = "Returns the count of games in the repository.")
    public long gameCount() {
        log.info("querying available games count...");
        return gameRepository.count();
    }

    @McpTool(name = "findGamesForPlayerCount",
            description = "Finds games suitable for the specified number of players.")
    public List<Game> findGamesForPlayerCount(
            @McpToolParam(description = "The number of players to find games for.") int numPlayers) {
        log.info("querying suitable game for the specified number of players - {}", numPlayers);
        return gameRepository.findGamesForPlayerCount(numPlayers);
    }

    @McpTool(name = "findGamesForPlayingTime",
            description = "Finds games suitable for the specified playing time.")
    public List<Game> findGamesForPlayingTime(
            @McpToolParam(description = "The time for playing the game.") int playingTime) {
        log.info("querying suitable game for the given play time in minutes - {}", playingTime);
        return gameRepository.findGamesForPlayingTime(playingTime);
    }
}
