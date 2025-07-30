package com.eazybytes.springai.controller;

import com.eazybytes.springai.exception.InvalidAnswerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SelfEvaluatingChatController {

    private final ChatClient chatClient;
    private final FactCheckingEvaluator factCheckingEvaluator;

    @Value("classpath:/promptTemplates/hrPolicy.st")
    Resource hrPolicyTemplate;

    public SelfEvaluatingChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.factCheckingEvaluator = new FactCheckingEvaluator(chatClientBuilder);
    }

    @Retryable(retryFor =  InvalidAnswerException.class,maxAttempts = 3)
    @GetMapping("/evaluate/chat")
    public String chat(@RequestParam("message") String message) {
        String aiResponse = chatClient.prompt().user(message)
                .call().content();
        validateAnswer(message, aiResponse);
        return aiResponse;
    }

    @GetMapping("/evaluate/prompt-stuffing")
    public String promptStuffing(@RequestParam("message") String message) {
        return chatClient
                .prompt().system(hrPolicyTemplate)
                .user(message)
                .call().content();
    }

    private void validateAnswer(String message, String answer) {
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, List.of(), answer);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }

    @Recover
    public String recover(InvalidAnswerException exception) {
        return "I'm sorry, I couldn't answer your question. Please try rephrasing it.";
    }

}
