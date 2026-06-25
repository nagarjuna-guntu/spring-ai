package com.example.toolscurrenttime;

import org.springframework.ai.tool.annotation.Tool;


public class TimeTools {

    @Tool(name = "getCurrentTime",
            description = "Get the current time in a specific timezone")
    public String getCurrentTime(String timeZone) {
        return java.time.LocalDateTime.now(java.time.ZoneId.of(timeZone)).toString();
    }
}
