package com.example.mcpclient.web;

import com.example.mcpclient.domain.Answer;
import com.example.mcpclient.domain.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
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

    public McpAskController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(SimpleLoggerAdvisor.builder().build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();

    }

    @PostMapping("/ask")
    public Answer ask(@RequestBody Question question) {
        log.info("calling chat client with the question - {}", question.question());
        var answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(question.question())
                .call()
                .content();
        log.info("The response from the mcp server - {}", answer);
        return new Answer(answer);
    }
}
