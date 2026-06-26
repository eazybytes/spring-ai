package com.eazybytes.mcpclient.util;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public class ToolUtil {

    /**
     * Keeps only the tools whose MCP server name and tool name match the given hints.
     * A null/blank hint means "match everything", same idea as the global
     * {@link McpServerToolFilter}, but applied per request.
     */
    public static ToolCallback[] selectToolsFor(List<McpSyncClient> mcpClients,
            String serverName, String toolName) {
        return mcpClients.stream()
                .flatMap(client -> client.listTools().tools().stream()
                        // getServerInfo().name() is the same value the global filter reads
                        .filter(tool -> matches(client.getServerInfo().name(), serverName)
                                && matches(tool.name(), toolName))
                        .map(tool -> (ToolCallback) SyncMcpToolCallback.builder()
                                .mcpClient(client)
                                .tool(tool)
                                .build()))
                .toArray(ToolCallback[]::new);
    }

    private static boolean matches(String actual, String hint) {
        return hint == null || hint.isBlank()
                || actual.toLowerCase().contains(hint.toLowerCase());
    }
}
