package com.example.metadata.web;


import com.example.metadata.domain.Answer;
import com.example.metadata.domain.BoardGameService;
import com.example.metadata.domain.Question;
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
