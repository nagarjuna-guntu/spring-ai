package com.example.mcpserver.domain;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameTools {

    private final GameRepository gameRepository;

    public GameTools(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Tool(name = "gameCount",
    description = "Returns the count of games in the repository.")
    public long getGameCount() {
        return gameRepository.count();
    }

    @Tool(name = "findGamesForPlayerCount",
    description = "Finds a games suitable for the specified number of players.")
    public List<Game> findGamesForPlayerCount(
            @ToolParam(description = "The number of players to find games for.") int numPlayers) {
        return gameRepository.findGamesForPlayerCount(numPlayers);
    }

    @Tool(name = "findGamesForPlayingTime",
    description = "Finds games suitable for the specified playing time.")
    public List<Game> findGamesForPlayingTime(
            @ToolParam(description = "The time for playing the game.") int time) {
        return gameRepository.findGamesForPlayingTime(time);
    }

}
