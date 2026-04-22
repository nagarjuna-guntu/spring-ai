package com.example.promptroles.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
@Slf4j
public class GameRulesService {

    public String loadGameRules(String gameName) {
        var fileName = String.format(
                "/gameRules/%s.txt",
                gameName.toLowerCase().replace(" ", "_")
        );
        log.info("trying to load file name : {}", fileName);
        try {
            return new DefaultResourceLoader()
                    .getResource(fileName)
                    .getContentAsString(Charset.defaultCharset());
        } catch (IOException e) {
            log.info("No rules found for game: {} ", gameName);
            return "";
        }
    }
}