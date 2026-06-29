package com.eazybytes.support.agent.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Full message detail returned by Mailpit's {@code /api/v1/message/{ID}}
 * endpoint. Note: fetching this endpoint marks the message as read server-side.
 */
public record MailpitMessage(
        @JsonProperty("ID") String id,
        @JsonProperty("MessageID") String messageId,
        @JsonProperty("From") MailpitAddress from,
        @JsonProperty("To") List<MailpitAddress> to,
        @JsonProperty("Subject") String subject,
        @JsonProperty("Date") String date,
        @JsonProperty("Text") String text,
        @JsonProperty("HTML") String html
) {
    public List<MailpitAddress> to() {
        return to != null ? to : List.of();
    }
}
