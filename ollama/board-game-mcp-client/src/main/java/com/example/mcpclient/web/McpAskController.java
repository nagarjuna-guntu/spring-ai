package com.example.mcpclient.web;

import com.example.mcpclient.domain.Answer;
import com.example.mcpclient.domain.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class McpAskController {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant, able to answer questions about board games,
            including how many players can play and how long a game typically takes
            to play.
            """;

    private final ChatClient chatClient;

    public McpAskController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public Answer ask(@RequestBody Question question) {
        log.info("calling chat client with the question - {}", question.question());
        var answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question.question())
                .call()
                .entity(Answer.class, entityParamSpec -> entityParamSpec
                        .useProviderStructuredOutput()
                        .validateSchema());
        log.info("The response from the mcp server - {}", answer);
        return (answer);
    }
}
