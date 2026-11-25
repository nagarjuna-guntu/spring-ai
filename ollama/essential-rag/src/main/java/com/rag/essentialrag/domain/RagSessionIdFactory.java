package com.rag.essentialrag.domain;

import org.springframework.stereotype.Component;

@Component
public class RagSessionIdFactory {

    public String forBoardGame(String username,
                               String gameId) {
        return "games:%s:%s".formatted(username, gameId);
    }

    public String forSpringDocs(String username) {
        return "spring-docs:%s".formatted(username);
    }
}
