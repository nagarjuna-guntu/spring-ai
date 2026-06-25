package com.example.mcpservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "game")
public record Game(
        @Id
        Long id,
        String title,
        String description,
        @Column("min_players") int minPlayers,
        @Column("max_players") int maxPlayers,
        @Column("min_playing_time") int minPlayingTimeMinutes,
        @Column("max_playing_time") int maxPlayingTimeMinutes
) {
    // Production safety: Validate state inside the entity constructor
    public Game {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title cannot be empty");
        if (minPlayers <= 0 || maxPlayers <= 0) throw new IllegalArgumentException("Players must be positive");
        if (maxPlayers < minPlayers) throw new IllegalArgumentException("Max players cannot be less than min players");
        if (minPlayingTimeMinutes <= 0 || maxPlayingTimeMinutes <= 0)
            throw new IllegalArgumentException("Playing times must be positive");
        if (maxPlayingTimeMinutes < minPlayingTimeMinutes)
            throw new IllegalArgumentException("Max playing time cannot be less than min playing time");
    }
}
