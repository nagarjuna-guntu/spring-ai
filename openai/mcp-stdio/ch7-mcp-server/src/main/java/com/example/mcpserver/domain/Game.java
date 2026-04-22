package com.example.mcpserver.domain;

import org.springframework.data.annotation.Id;

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
