package com.rag.hyde.web.game;

import com.rag.hyde.domain.game.BoardGameService;
import com.rag.hyde.web.AssistantResponse;

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
        log.info("Asking question: [{}] for user: [{}]", question.question(), loggedInUser);
        String answer = boardGameService.ask(question, loggedInUser);
        return new AssistantResponse(answer);
    }
}
