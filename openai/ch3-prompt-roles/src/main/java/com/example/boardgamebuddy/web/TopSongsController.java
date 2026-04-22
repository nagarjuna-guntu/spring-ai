package com.example.boardgamebuddy.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TopSongsController {

    @Value("classpath:/top-songs-prompt.st")
    Resource topSongPromptTemplate;

    private final ChatClient chatClient;

    public TopSongsController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public List<String> topSongs(@RequestParam(value = "year", defaultValue = "2026") String year) {
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(topSongPromptTemplate)
                        .param("year", year))
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {});
    }
}
