package com.example.boardgamebuddy.weather;

import java.util.function.Function;

public class WeatherService implements Function<WeatherRequest, WeatherResponse> {
    @Override
    public WeatherResponse apply(WeatherRequest weatherRequest) {
        return new WeatherResponse(30.2, Unit.C);
    }
}
