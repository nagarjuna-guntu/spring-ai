package com.example.boardgamebuddy;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Predicate;

@RestController
@RequestMapping("ask")
public class AskController {
    private final BoardGameBuddyService boardGameBuddyService;

    public AskController(BoardGameBuddyService boardGameBuddyService) {
        this.boardGameBuddyService = boardGameBuddyService;
    }

    @PostMapping
    public Answer ask(@RequestBody Question question) {
        return boardGameBuddyService.ask(question);
    }
}
