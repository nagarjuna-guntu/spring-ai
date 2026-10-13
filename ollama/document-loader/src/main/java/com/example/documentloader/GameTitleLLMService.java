package com.example.documentloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameTitleLLMService {
    private final ChatClient chatClient;
    private final PromptResources promptResources;

    public GameTitleLLMService(ChatClient chatClient, PromptResources promptResources) {
        this.chatClient = chatClient;
        this.promptResources = promptResources;
    }

    public GameTitle findGameTitle(String combinedText) {

        return chatClient.prompt().user(promptUserSpec -> promptUserSpec.text(promptResources.get("userPrompt")).param("document", combinedText)).options(OllamaChatOptions.builder().temperature(0.0).topP(0.1)).call().entity(GameTitle.class, entityParamSpec -> entityParamSpec.useProviderStructuredOutput().validateSchema());
    }
}
