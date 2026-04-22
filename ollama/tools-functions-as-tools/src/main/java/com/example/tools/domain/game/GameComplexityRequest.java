package com.example.tools.domain.game;

import org.springframework.ai.tool.annotation.ToolParam;


public record GameComplexityRequest(
        @ToolParam(description = "The title of the game") String title
) {
}
