package com.example.boardgamebuddy.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Primary
public class SpringAiBoardGameService implements BoardGameService {

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource promptTemplate;


    private final ChatClient chatClient;


    public SpringAiBoardGameService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();

    }

    @Override
    public Answer askQuestion(Question question, String conversationId) {
        var gameNameMatchExpression = String.format(
                "gameTitle == '%s'", normalizeGameTitle(question.gameTitle()));
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec
                        .text(promptTemplate)
                        .param("gameTitle", question.gameTitle()))
                .user(question.question())
                .advisors(advisorSpec -> advisorSpec
                        .param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, gameNameMatchExpression)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(Answer.class);
    }

    private String normalizeGameTitle(String in) {
        return in.toLowerCase().replace(' ', '_');
    }
}
