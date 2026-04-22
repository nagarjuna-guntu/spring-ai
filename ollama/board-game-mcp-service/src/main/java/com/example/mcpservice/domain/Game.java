package com.example.mcpservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "game")
public record Game(
        @Id
        Long id,
        String title,
        String description,
        Integer minPlayers,
        Integer maxPlayers,
        Integer minPlayingTime,
        Integer maxPlayingTime
) {
}
