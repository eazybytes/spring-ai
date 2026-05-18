package com.eazybytes.springai.controller;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ImageController {

    private final OpenAiImageModel openAiImageModel;

    public ImageController(OpenAiImageModel openAiImageModel) {
        this.openAiImageModel = openAiImageModel;
    }

    @GetMapping("/image")
    String genateImage(@RequestParam("message") String message) {
        var imageResponse = openAiImageModel.call(new ImagePrompt(message,
                OpenAiImageOptions.builder()
                        .model("gpt-image-2").build()));
        return imageResponse.getResults().get(0).getOutput().getB64Json();
    }

    @GetMapping("/image-options")
    String genateImageWithOptions(@RequestParam("message") String message) {
        var imageResponse = openAiImageModel.call(new ImagePrompt(message,
                OpenAiImageOptions.builder()
                        .N(1)
                        .model("gpt-image-2").build()));
        return imageResponse.getResults().get(0).getOutput().getB64Json();
    }

}
