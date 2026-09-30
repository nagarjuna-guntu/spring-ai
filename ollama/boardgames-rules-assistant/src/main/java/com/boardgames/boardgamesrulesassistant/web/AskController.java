package com.boardgames.boardgamesrulesassistant.web;

import com.boardgames.boardgamesrulesassistant.domain.BoardGameService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ask")
@Slf4j
public class AskController {

    private final BoardGameService boardGameService;

    public AskController(BoardGameService boardGameService) {

        this.boardGameService = boardGameService;
    }

    @PostMapping
    public Answer ask(@AuthenticationPrincipal UserDetails user,
                      @RequestBody @Valid Question question,
                      @RequestHeader(name = "X_AI_CHAT_ID", defaultValue = "default-user") String chatId) {

        String loggedInUserChatId = user.getUsername() + "_" + chatId;

        log.info("ask question - [{}] : user chat id - [{}]", question, loggedInUserChatId);

        return boardGameService.askQuestion(question, loggedInUserChatId);
    }
}
