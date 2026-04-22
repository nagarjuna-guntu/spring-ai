package com.example.tools.domain.game;

public enum GameComplexity {

    UNKNOWN(0),
    EASY(1),
    MODERATELY_EASY(2),
    MODERATE(3),
    MODERATELY_DIFFICULT(4),
    DIFFICULT(5);

    private final int value;

    GameComplexity(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
