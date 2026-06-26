package com.eazybytes.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpElicitation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HelpDeskElicitationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskElicitationProvider.class);

    @McpElicitation(clients = "eazybytes")
    public McpSchema.ElicitResult handleElicitationRequest(McpSchema.ElicitRequest request) {
        LOGGER.info("Received MCP elicitation request from server: {}", request.message());
        // Simulate a human filling in the requested form. The keys here must match the
        // field names of the server's requested schema (TicketContactInfo: priority, contactPhone).
        Map<String, Object> userResponse = Map.of(
                "priority", "HIGH",
                "contactPhone", "+1-202-555-0185");

        LOGGER.info("Responding to elicitation with ACCEPT and data: {}", userResponse);
        return McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                .content(userResponse)
                .build();
    }
}
