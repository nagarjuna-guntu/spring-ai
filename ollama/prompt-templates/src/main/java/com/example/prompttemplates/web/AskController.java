package com.example.prompttemplates.web;

import com.example.prompttemplates.domain.Answer;
import com.example.prompttemplates.domain.BoardGameService;
import com.example.prompttemplates.domain.Question;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AskController {
    private final BoardGameService boardGameService;

    public AskController(BoardGameService boardGameService) {
        this.boardGameService = boardGameService;
    }

    @PostMapping(path = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public Answer ask(@RequestBody @Valid Question question) {
        return boardGameService.askQuestion(question);
    }
}
