package com.example.boardgamebuddy.domain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class SelfEvaluatingBoardGameService implements BoardGameService{

    private final ChatClient chatClient;
    private final RelevancyEvaluator relevancyEvaluator;

    public SelfEvaluatingBoardGameService(ChatClient.Builder chatClientBuilder) {
        ChatOptions chatOptions = ChatOptions.builder()
                .model("gpt-5.4-mini")
                .build();
        this.chatClient = chatClientBuilder
                .defaultOptions(chatOptions)
                .build();
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(3).includes(AnswerNotRelevantException.class).build();

    }

    @Override
    @Retryable(includes = AnswerNotRelevantException.class,
    maxRetries = 3)
    public Answer askQuestion(Question question, String conversationId) {
        var answerText = chatClient.prompt()
                .user(question.question())
                .call()
                .content();
        evaluateRelevancy(question, answerText);
        return new Answer(answerText);
    }

    private void evaluateRelevancy(Question question, String answerText) {
        EvaluationRequest request = new EvaluationRequest(question.question(), answerText);
        EvaluationResponse response = relevancyEvaluator.evaluate(request);
        if (!response.isPass()) {
            throw new AnswerNotRelevantException(question.question(), answerText);
        }
    }
}
