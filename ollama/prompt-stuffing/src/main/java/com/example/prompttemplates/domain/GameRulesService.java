package com.example.prompttemplates.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
@Slf4j
public class GameRulesService {

    private final ResourceLoader resourceLoader;

    public GameRulesService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadGameRules(String gameName) {
        var fileName = String.format(
                "classpath:/gameRules/%s.txt",
                gameName.toLowerCase().replace(" ", "_")
        );
        log.info("trying to load file name : {}", fileName);
        try {
            return resourceLoader
                    .getResource(fileName)
                    .getContentAsString(Charset.defaultCharset());
        } catch (IOException e) {
            log.info("No rules found for game: {} ", gameName);
            return "";
        }
    }
}