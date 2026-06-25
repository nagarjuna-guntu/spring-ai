package com.example.summarizingcontent.domain;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BoardGameService {

    private final ChatClient chatClient;
    private final Resource promptTemplate;
    private final Resource summarizeTemplate;

    public BoardGameService(ChatClient chatClient,
                            @Value("classpath:/promptTemplates/systemPrompt.st") Resource promptTemplate,
                            @Value("classpath:/promptTemplates/summarizePrompt.st") Resource summarizeTemplate) {
        this.chatClient = chatClient;
        this.promptTemplate = promptTemplate;
        this.summarizeTemplate = summarizeTemplate;
    }

    public Answer askQuestion(Question question) {
        log.info("ask Question gameTitle - {}", question.normalizeTitle());
        var gameNameMatchExpression = String.format(
                "gameTitle == '%s'", question.normalizeTitle());


        var answerText = chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle())
                )
                .user(question.question())
                .advisors(advisorSpec -> advisorSpec
                        .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, gameNameMatchExpression)
                )
                .call()
                .content();
        return new Answer(question.gameTitle(), answerText);

    }

    public Answer summarizeRules(String text) {
        log.info("summarizeRules ...");

        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(summarizeTemplate))
                .user(promptUserSpec -> promptUserSpec
                        .text("Please summarize the following document:\n{document}")
                        .param("document", text))
                .call()
                .entity(Answer.class, entityParamSpec -> entityParamSpec
                        .useProviderStructuredOutput()
                        .validateSchema());

    }
}
