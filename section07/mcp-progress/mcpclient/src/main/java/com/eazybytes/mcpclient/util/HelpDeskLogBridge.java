package com.eazybytes.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpLogging;
import org.springframework.stereotype.Component;

@Component
public class HelpDeskLogBridge {

    private static final Logger log = LoggerFactory.getLogger(HelpDeskLogBridge.class);

    @McpLogging(clients = "eazybytes")
    public void onServerLog(McpSchema.LoggingLevel level,
            String source,
            String message) {
        log.info("Received log from server - Level: {}, Source: {}, Message: {}", level, source, message);
    }
}
