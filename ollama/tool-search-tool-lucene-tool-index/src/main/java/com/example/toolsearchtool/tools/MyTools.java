package com.example.toolsearchtool.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.util.List;

public class MyTools {

    @Tool(description = "Get the weather for a given location and at a given time")
    public String weather(String location, @ToolParam(description = "YYYY-MM-DDTHH:mm:ss") String atTime) {
        return "The current weather in " + location + " is sunny with a temperature of 25°C.";
    }

    @Tool(description = "Get the names of clothing shops for a given location and at a given time")
    public List<String> clothing(String location,
                                 @ToolParam(description = "YYYY-MM-DDTHH:mm:ss") String openAtTime) {
        return List.of("Foo", "Bar", "Baz");
    }

    @Tool(description = "Provides the current date and time (as date-time string) for a given location")
    public String currentTime(String location) {
        return LocalDateTime.now().toString();
    }

}
