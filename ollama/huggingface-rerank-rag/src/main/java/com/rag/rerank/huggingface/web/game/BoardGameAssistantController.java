package com.rag.rerank.huggingface.web.game;

import com.rag.rerank.huggingface.domain.game.BoardGameService;
import com.rag.rerank.huggingface.web.AssistantResponse;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant/games")
@Slf4j
public class BoardGameAssistantController {
    private final BoardGameService boardGameService;

    public BoardGameAssistantController(BoardGameService boardGameService) {
        this.boardGameService = boardGameService;
    }

    @PostMapping("/ask")
    public AssistantResponse askQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid BoardGameQuestion question) {
        String loggedInUser = userDetails.getUsername();

        log.atInfo()
                .addKeyValue("documentType", "RULEBOOK")
                .addKeyValue("question", question.question())
                .addKeyValue("loggedInUser", loggedInUser)
                .log("Asking question...");
        return boardGameService.ask(question, loggedInUser);

    }
}
