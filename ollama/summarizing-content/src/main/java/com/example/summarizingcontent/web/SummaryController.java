package com.example.summarizingcontent.web;

import com.example.summarizingcontent.domain.Answer;
import com.example.summarizingcontent.domain.BoardGameService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

@RestController
public class SummaryController {

    private final BoardGameService boardGameService;

    public SummaryController(BoardGameService boardGameService) {
        this.boardGameService = boardGameService;
    }

    @PostMapping("/summarize")
    public Answer summarize(@RequestPart("rulesDocument") MultipartFile rulesDocument) {
        var reader = new TikaDocumentReader(rulesDocument.getResource());
        var text = reader.get().stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        return boardGameService.summarizeRules(text);
    }
}
