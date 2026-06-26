package com.eazybytes.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.annotation.McpSampling;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handles MCP <b>sampling</b> requests. When a tool on the MCP server calls
 * {@code ctx.sample(...)}, the server forwards a {@link McpSchema.CreateMessageRequest}
 * to this client. We run the requested completion against our own LLM and return the
 * result, giving the server LLM access without it needing its own API key.
 * <p>
 * IMPORTANT: we inject the low-level {@link ChatModel} and NOT a {@code ChatClient}.
 * A {@code ChatClient} is wired up with the MCP tool callbacks, so using it here could
 * trigger another tool call that issues yet another sampling request -> infinite loop.
 */
@Component
public class HelpDeskSamplingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskSamplingProvider.class);

    private final ChatModel chatModel;

    public HelpDeskSamplingProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @McpSampling(clients = "eazybytes")
    public McpSchema.CreateMessageResult handleSamplingRequest(McpSchema.CreateMessageRequest request) {
        LOGGER.info("Received MCP sampling request from server. System prompt: {}", request.systemPrompt());

        // Translate the MCP sampling messages into Spring AI prompt messages.
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        String userText = request.messages().stream()
                .filter(m -> m.content() instanceof McpSchema.TextContent
                        && m.role().name().equalsIgnoreCase(McpSchema.Role.USER.name()))
                .map(m -> ((McpSchema.TextContent) m.content()).text())
                .collect(Collectors.joining("\n"));
        messages.add(new UserMessage(userText));

        // Call the LLM directly via ChatModel to avoid re-triggering MCP tools.
        ChatResponse response = chatModel.call(new Prompt(messages));
        if (response.getResult() == null) {
            throw new IllegalStateException("LLM returned no result for the MCP sampling request");
        }
        String generatedText = Objects.requireNonNullElse(response.getResult().getOutput().getText(), "");
        String model = response.getMetadata().getModel();
        LOGGER.info("LLM produced sampling response using model '{}': {}", model, generatedText);

        return McpSchema.CreateMessageResult.builder(McpSchema.Role.ASSISTANT,generatedText,model)
                .build();
    }
}
