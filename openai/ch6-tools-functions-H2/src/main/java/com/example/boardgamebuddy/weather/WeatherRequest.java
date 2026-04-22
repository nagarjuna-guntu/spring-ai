package com.example.boardgamebuddy.weather;

import org.springframework.ai.tool.annotation.ToolParam;

public record WeatherRequest(
        @ToolParam(description = "The name of a city or a country") String location,
        Unit unit) {
}
