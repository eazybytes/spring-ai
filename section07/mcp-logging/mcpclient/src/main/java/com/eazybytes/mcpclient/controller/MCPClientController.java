package com.eazybytes.mcpclient.controller;

import com.eazybytes.mcpclient.util.ToolUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                .call().content();
    }

}
