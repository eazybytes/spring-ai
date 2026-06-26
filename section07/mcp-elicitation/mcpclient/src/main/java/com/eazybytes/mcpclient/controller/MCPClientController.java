package com.eazybytes.mcpclient.controller;

import com.eazybytes.mcpclient.util.ToolUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class MCPClientController {

    private  final ChatClient chatClient;
    private final List<McpSyncClient> mcpClients;

    public MCPClientController(ChatClient.Builder chatClientBuilder,
            List<McpSyncClient> mcpClients
            /*ToolCallbackProvider toolCallbackProvider*/) {
        this.chatClient = chatClientBuilder/*.defaultTools(toolCallbackProvider)*/
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.mcpClients = mcpClients;
    }

    @GetMapping("/chat")
    public String chat(@RequestHeader(value = "username",required = false) String username,
            @RequestParam("message") String message) {
        // filter by MCP server name (connection name) and tool name
        ToolCallback[] toolCallbacks = ToolUtil.selectToolsFor(mcpClients, "helpdesk-mcp-server", null);
        return chatClient.prompt().user(message+ " My username is " + username)
                .tools(toolCallbacks)
                .toolContext(Map.of("progressToken", UUID.randomUUID().toString()))
                .call().content();
    }

    /**
     * Dedicated endpoint for the MCP sampling demo. We still let the chat LLM drive: it sees
     * the {@code summarizeTickets} tool and decides to call it. That tool runs on the server,
     * which then calls back into this client's {@code @McpSampling} handler to generate the
     * natural-language summary. The controller never invokes the tool directly.
     */
    @GetMapping("/summarize-tickets")
    public String summarizeTickets(@RequestHeader("username") String username) {
        ToolCallback[] toolCallbacks = ToolUtil.selectToolsFor(mcpClients, "helpdesk-mcp-server", null);
        return chatClient.prompt()
                .system("""
                        You orchestrate the 'summarizeTickets' tool. The tool already returns a complete,
                        customer-ready summary that was generated for this exact request. Return that tool
                        output to the user EXACTLY as-is: do not rewrite, reformat, shorten, expand,
                        rephrase, or add any commentary of your own. Your reply must be the verbatim tool
                        response and nothing else.
                        """)
                .user("Summarize all of my support tickets. My username is " + username)
                .tools(toolCallbacks)
                .toolContext(Map.of("progressToken", UUID.randomUUID().toString()))
                .call().content();
    }

}
