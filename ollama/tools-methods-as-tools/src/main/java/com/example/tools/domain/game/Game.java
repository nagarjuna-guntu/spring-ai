package com.example.tools.domain.game;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "game")
public record Game(
        @Id
        Long id,
        String slug,
        String title,
        float complexity
) {
    public static final Game UNKOWN_GAME = Game.of("unknownSlug", "unknownTitle", GameComplexity.UNKNOWN.getValue());

    public GameComplexity complexityEnum() {
        int rounded = Math.round(complexity);
        return GameComplexity.values()[rounded];
    }

    public static Game of(String slug, String title, float complexity) {
        return new Game(null, slug, title, complexity);
    }
}
