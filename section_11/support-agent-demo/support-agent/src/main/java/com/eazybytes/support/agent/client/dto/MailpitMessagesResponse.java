package com.eazybytes.support.agent.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Envelope returned by Mailpit's {@code /api/v1/messages} and
 * {@code /api/v1/search} endpoints.
 */
public record MailpitMessagesResponse(
        @JsonProperty("total") int total,
        @JsonProperty("unread") int unread,
        @JsonProperty("count") int count,
        @JsonProperty("messages") List<MailpitMessageSummary> messages
) {
    public List<MailpitMessageSummary> messages() {
        return messages != null ? messages : List.of();
    }
}
