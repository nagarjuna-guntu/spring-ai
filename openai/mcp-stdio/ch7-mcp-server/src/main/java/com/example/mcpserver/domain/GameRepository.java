package com.example.mcpserver.domain;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface GameRepository extends ListCrudRepository<Game, Long> {
    @Query("""
      SELECT id, title, description, min_players, max_players,
             min_playing_time, max_playing_time 
      FROM Game
      WHERE min_players <= :numPlayers AND max_players >= :numPlayers
        """)
    List<Game> findGamesForPlayerCount(int numPlayers);

    @Query("""
      SELECT id, title, description, min_players, max_players,
             min_playing_time, max_playing_time
      FROM game
      WHERE min_playing_time <= :minutes AND max_playing_time >= :minutes
      """)
    List<Game> findGamesForPlayingTime(int minutes);
}
