package com.example.boardgamebuddy.gamedata;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Description;

@Description("Request data about a game, given the game title.")
public record GameComplexityRequest(
        @ToolParam(description = "The title of the game") String title) {
}
