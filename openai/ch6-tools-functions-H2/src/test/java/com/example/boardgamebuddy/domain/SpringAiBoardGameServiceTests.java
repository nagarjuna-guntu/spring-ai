package com.example.boardgamebuddy.domain;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class SpringAiBoardGameServiceTests {

    @Autowired
    private BoardGameService boardGameService;

    @Autowired
    private ChatClient.Builder chatClBuilder;

    private RelevancyEvaluator relevancyEvaluator;
    private FactCheckingEvaluator factCheckingEvaluator;

    @BeforeEach
    public void setUp() {

        this.relevancyEvaluator = new RelevancyEvaluator(chatClBuilder);
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClBuilder).build();
    }

    @Test
    void evaluateRelevancy() {
        String questionText = "Why is the sky blue?";           //Arrange
        Question question = new Question(questionText);         //Arrange
        Answer answer = boardGameService.askQuestion(question, conversationId); //Act
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

    @Test
    void evaluateFactualAccuracy() {
        var questionText = "Why is the sky blue?";           //Arrange
        var question = new Question(questionText);         //Arrange
        var answer = boardGameService.askQuestion(question, conversationId); //Act
        var evaluationRequest = new EvaluationRequest(questionText, answer.answer());
        var evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
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