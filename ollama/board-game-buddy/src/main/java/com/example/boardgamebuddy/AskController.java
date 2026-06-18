package com.example.boardgamebuddy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("ask")
public class AskController {
    private final BoardGameBuddyService boardGameBuddyService;

    public AskController(BoardGameBuddyService boardGameBuddyService) {
        this.boardGameBuddyService = boardGameBuddyService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Answer ask(@RequestBody Question question) {
        return boardGameBuddyService.ask(question);
    }
}
