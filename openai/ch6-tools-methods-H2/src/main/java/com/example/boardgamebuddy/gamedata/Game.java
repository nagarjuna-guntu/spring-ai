package com.example.boardgamebuddy.gamedata;

import org.springframework.data.annotation.Id;

public record Game(
        @Id Long id,
        String slug,
        String title,
        float complexity
) {
    public static final Game UNKOWN_GAME = Game.of("unknownSlug", "unknownTitle", GameComplexity.UNKNOWN.getValue());

    public GameComplexity gameComplexity() {
        int rounded = Math.round(complexity);
        return GameComplexity.values()[rounded];
    }

    public static Game of(String slug, String title, float complexity) {
        return new Game(null, slug, title, complexity);
    }
}
