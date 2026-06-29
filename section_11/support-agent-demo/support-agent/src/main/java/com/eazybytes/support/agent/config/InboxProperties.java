package com.eazybytes.support.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the inbox the agent monitors via the Mailpit REST API.
 * Bound from the {@code support-agent.inbox.*} properties.
 *
 * @param baseUrl      base URL of the Mailpit HTTP API/UI (e.g. http://localhost:8025)
 * @param address      the support mailbox address; used as the recipient when seeding test mail
 * @param pollInterval how often to poll for new mail, in milliseconds
 * @param batchSize    max number of unread messages to pull per poll
 */
@ConfigurationProperties(prefix = "support-agent.inbox")
public record InboxProperties(
        String baseUrl,
        String address,
        long pollInterval,
        int batchSize
) {
    public InboxProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8025";
        }
        if (address == null || address.isBlank()) {
            address = "support@example.com";
        }
        if (batchSize <= 0) {
            batchSize = 50;
        }
    }
}
