package com.eazybytes.springai.controller;

import com.openai.models.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PromptStuffingController {

    private final ChatClient chatClient;

    public PromptStuffingController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Value("classpath:/promptTemplates/systemPromptTemplate.st")
    Resource systemPromptTemplate;

    @GetMapping("/prompt-stuffing")
    public String promptStuffing(@RequestParam("message") String message) {
        return chatClient
                .prompt()
                .options(OpenAiChatOptions.builder().model(ChatModel.GPT_5_4_NANO.asString())
                        .temperature(0.7))
                .system(systemPromptTemplate)
                .user(message)
                .call().content();
    }

}
