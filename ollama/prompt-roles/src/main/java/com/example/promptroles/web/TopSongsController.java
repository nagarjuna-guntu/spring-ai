package com.example.promptroles.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TopSongsController {

    @Value("classpath:/promptTemplates/top-songs-prompt.st")
    Resource topSongPromptTemplate;

    private final ChatClient chatClient;

    public TopSongsController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/top-songs")
    public String topSongs(@RequestParam(name = "year", defaultValue = "2000") String year) {
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec
                        .text(topSongPromptTemplate)
                        .param("year", year))
                .call()
                .content();
    }
}
