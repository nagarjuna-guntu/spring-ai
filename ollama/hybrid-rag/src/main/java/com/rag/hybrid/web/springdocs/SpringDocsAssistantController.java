package com.rag.hybrid.web.springdocs;

import com.rag.hybrid.domain.springdocs.SpringDocsService;
import com.rag.hybrid.web.AssistantResponse;

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

        log.atInfo()
                .addKeyValue("loggedInUser", loggedInUser)
                .addKeyValue("question", question.question())
                .addKeyValue("documentType", "Spring_Boot_Docs")
                .log("Asking question.");

        return springDocsService.ask(question, loggedInUser);
    }
}
