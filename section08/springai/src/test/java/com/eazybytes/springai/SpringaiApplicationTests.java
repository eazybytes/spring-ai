package com.eazybytes.springai;

import com.eazybytes.springai.controller.ChatController;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=${OPENAI_API_KEY:test-key}",
        "logging.level.org.springframework.ai=DEBUG"
})
class SpringaiApplicationTests {

    @Autowired
    private ChatController chatController;

    @Autowired
    private ChatModel chatModel;

    private ChatClient chatClient;
    private RelevancyEvaluator relevancyEvaluator;
    private FactCheckingEvaluator factCheckingEvaluator;

    // Minimum acceptable relevancy score
    @Value("${test.relevancy.min-score:0.7}")
    private float minRelevancyScore;

    @Value("classpath:/promptTemplates/hrPolicy.st")
    Resource hrPolicyTemplate;

    @BeforeEach
    void setup() {
        ChatClient.Builder chatClientBuilder =
                ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor());
        this.chatClient = chatClientBuilder.build();
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.factCheckingEvaluator = new FactCheckingEvaluator(chatClientBuilder);
    }

    @Test
    @DisplayName("Should return relevant response for basic geography question")
    @Timeout(value = 30)
    void evaluateChatControllerResponseRelevancy() {
        // Given
        String question = "What is the capital of India ?";

        // When
        String aiResponse = chatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse response = relevancyEvaluator.evaluate(evaluationRequest);

        Assertions.assertAll(() -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(response.isPass())
                        .withFailMessage("""
                                ========================================
                                The answer was not considered relevant.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse)
                        .isTrue(),
                () -> assertThat(response.getScore())
                        .withFailMessage("""
                                ========================================
                                The score %.2f is lower than the minimum required %.2f.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, response.getScore(), minRelevancyScore, question, aiResponse)
                        .isGreaterThan(minRelevancyScore));

    }

    @Test
    @DisplayName("Should return factually correct response for gravity-related question")
    @Timeout(value = 30)
    void evaluateFactAccuracyForGravityQuestion() {
        // Given
        String question = "Who discovered the law of universal gravitation?";

        // When
        String aiResponse = chatController.chat(question);
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse response = factCheckingEvaluator.evaluate(evaluationRequest);

        Assertions.assertAll(() -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(response.isPass())
                        .withFailMessage("""
                             ========================================
                             The answer was not considered factually correct.
                             Question: "%s"
                             Response: "%s"
                             ========================================
                                """, question, aiResponse)
                        .isTrue());

    }

    @Test
    @DisplayName("Should correctly evaluate factual response based on HR policy context (RAG scenario)")
    @Timeout(value = 30)
    public void evaluateHrPolicyAnswerWithRagContext() throws IOException {
        // Given
        String question = "How many paid leaves do employees get annually?";

        // When
        String aiResponse = chatController.promptStuffing(question);

        String retrievedContext = hrPolicyTemplate.getContentAsString(StandardCharsets.UTF_8);

        EvaluationRequest evaluationRequest = new EvaluationRequest(
                question,
                List.of(new Document(retrievedContext)),
                aiResponse
        );

        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        // Then
        Assertions.assertAll(
                () -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass())
                        .withFailMessage("""
                        ========================================
                        The response was not considered factually accurate.
                        Question: %s
                        Response: %s
                        Context: %s
                        ========================================
                        """, question, aiResponse, retrievedContext)
                        .isTrue());
    }

}
