package com.example.boardgamebuddy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BoardGameBuddyServiceTest {

    @Autowired
    private BoardGameBuddyService boardGameBuddyService;

    @Autowired
    private ChatClient.Builder chatClienBuilder;

    private RelevancyEvaluator relevancyEvaluator;

    @BeforeEach
    public void setUp() {

        this.relevancyEvaluator = new RelevancyEvaluator(chatClienBuilder);
    }

    @Test
    void evaluateRelevancy() {
        String questionText = "Why is the sky blue?";           //Arrange
        Question question = new Question(questionText);         //Arrange
        Answer answer = boardGameBuddyService.ask(question); //Act
        EvaluationRequest evaluationRequest = new EvaluationRequest(questionText, answer.answer());  //Evaluate(Arrange)
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);     //Evaluate(ACT)
        Assertions.assertThat(evaluationResponse.isPass())                                         // Assert
                .withFailMessage("""
                        ===========================================
                        The answer "%s" is not considered correct
                        to the question "%s".
                        ===========================================
                        """, answer.answer(), questionText)
                .isTrue();
    }

}