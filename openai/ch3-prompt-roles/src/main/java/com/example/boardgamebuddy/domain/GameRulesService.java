package com.example.boardgamebuddy.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
@Slf4j
public class GameRulesService {

    public String getRulesFor(String gameName) {
        try {
            var filename = String.format(
                    "classpath:/gameRules/%s.txt",
                    gameName.toLowerCase().replace(" ", "_"));

            return new DefaultResourceLoader()
                    .getResource(filename)
                    .getContentAsString(Charset.defaultCharset());
        } catch (IOException e) {
            log.info("No rules found for game: {} ", gameName);
            return "";
        }
    }
}
