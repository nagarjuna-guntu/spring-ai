package com.rag.essentialrag.domain.springdocs;

import com.rag.essentialrag.domain.RagSessionIdFactory;
import com.rag.essentialrag.web.springdocs.SpringDocsQuestion;

import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SpringDocsService {
    private static final String SYSTEM_PROMPT =
            """
            You are a Spring Boot documentation assistant.

            Answer technical questions using only the retrieved
            Spring Boot documentation.

            Instructions:
            1. Understand the user's problem, even when described
               using informal or non-technical language.
            2. Identify relevant Spring Boot features and explain
               how they address the problem.
            3. Provide practical implementation guidance and
               configuration examples when the retrieved context
               supports them.
            4. Do not invent Spring Boot APIs, properties,
               annotations, or configuration.
            5. If the retrieved context does not contain enough
               information, clearly state that.
            6. Prefer explanations appropriate for a Java
               Spring Boot developer.
            """;

    private final ChatClient chatClient;
    private final Advisor springDocRagAdvisor;
    private final RagSessionIdFactory sessionIdFactory;

    public SpringDocsService(
            ChatClient chatClient,
            @Qualifier("springDocRagAdvisor") Advisor springDocRagAdvisor,
            RagSessionIdFactory sessionIdFactory) {
        this.chatClient = chatClient;
        this.springDocRagAdvisor = springDocRagAdvisor;
        this.sessionIdFactory = sessionIdFactory;
    }

    public String ask(SpringDocsQuestion question, String loggedInUser) {
        String filterExpression = "documentType == 'SPRING_BOOT_DOC'";
        String sessionId = sessionIdFactory.forSpringDocs(loggedInUser);
        log.info(
                "Asking question: [{}] with filter expression: [{}] with the session-id: [{}]",
                question.question(),
                filterExpression,
                sessionId);

        return chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(question.question())
                .advisors(springDocRagAdvisor)
                .advisors(advisorSpec ->
                        advisorSpec.param(
                                QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression))
                .advisors(SessionAdvisors.session(sessionId))
                .call()
                .content();
    }
}
