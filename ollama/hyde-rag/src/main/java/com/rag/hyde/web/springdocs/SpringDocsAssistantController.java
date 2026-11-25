package com.rag.hyde.web.springdocs;

import com.rag.hyde.domain.springdocs.SpringDocsService;
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
@RequestMapping("/api/assistant/springdocs")
@Slf4j
public class SpringDocsAssistantController {
    private final SpringDocsService springDocsService;

    public SpringDocsAssistantController(SpringDocsService springDocsService) {
        this.springDocsService = springDocsService;
    }

    @PostMapping("/ask")
    public AssistantResponse askQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid SpringDocsQuestion question) {
        String loggedInUser = userDetails.getUsername();
        log.info("Asking question: [{}] for the user: [{}]", question.question(), loggedInUser);
        String answer = springDocsService.ask(question, loggedInUser);
        return new AssistantResponse(answer);
    }
}
