package com.boardgames.boardgamesrulesassistant.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateResource {
    private static final String BASE_PATH = "classpath:/prompts/";
    private final ResourceLoader resourceLoader;

    public PromptTemplateResource(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Resource get(String promptName) {
        return resourceLoader.getResource(BASE_PATH + promptName + ".st");
    }
}
