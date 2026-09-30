package com.example.documentloader;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class PromptResources {

    private static final String BASE_PATH = "classpath:/promptTemplates/";

    private final ResourceLoader resourceLoader;

    public PromptResources(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Resource get(String promptName) {
        return resourceLoader.getResource(BASE_PATH + promptName + ".st");
    }
}
