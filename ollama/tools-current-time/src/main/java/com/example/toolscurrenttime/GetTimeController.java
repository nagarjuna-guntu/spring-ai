package com.example.toolscurrenttime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/current-time")
public class GetTimeController {
    private static final String CURRENT_TIME_TEMPLATE =
            "What is the current time in {city}?";

    private final ChatClient chatClient;

    public GetTimeController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultTools(new TimeTools())
                .build();
    }

    @GetMapping
    public String getCurrentTime(@RequestParam(value = "city", defaultValue = "Pune") String city) {
        log.info("Getting current time from city: {}", city);
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(CURRENT_TIME_TEMPLATE)
                        .param("city", city))
                .call()
                .content();
    }
}
