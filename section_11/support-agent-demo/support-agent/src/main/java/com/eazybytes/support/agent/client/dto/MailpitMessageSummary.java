package com.eazybytes.support.agent.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A lightweight message entry returned by Mailpit's list/search endpoints.
 * Only the {@code ID} is needed to fetch the full message; the rest is handy
 * for logging.
 */
public record MailpitMessageSummary(
        @JsonProperty("ID") String id,
        @JsonProperty("MessageID") String messageId,
        @JsonProperty("Read") boolean read,
        @JsonProperty("From") MailpitAddress from,
        @JsonProperty("Subject") String subject,
        @JsonProperty("Created") String created
) {
}
