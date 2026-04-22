package com.example.boardgamebuddy.gamedata;

public record GameComplexityResponse(String title, GameComplexity gameComplexity) {
    public static GameComplexityResponse of(String title, GameComplexity gameComplexity) {
        return new GameComplexityResponse(title, gameComplexity);

    }
}
