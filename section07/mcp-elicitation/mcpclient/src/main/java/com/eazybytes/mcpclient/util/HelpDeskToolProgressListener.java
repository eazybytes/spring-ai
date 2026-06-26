package com.eazybytes.mcpclient.util;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpProgress;
import org.springframework.stereotype.Component;

@Component
public class HelpDeskToolProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskToolProgressListener.class);

    @McpProgress(clients = "eazybytes")
    public void onProgress(McpSchema.ProgressNotification notification) {
        LOGGER.info("Progress update - {}% complete received for Request ID {}: Message: {}",
                notification.progress(),
                notification.progressToken(),
                notification.message());
    }
}
