package com.eazybytes.support.agent.model;

import java.time.Instant;
import java.util.List;

/**
 * A clean, framework-agnostic view of a single email pulled from the inbox.
 * This is what the rest of the agent reasons about - we never leak the raw
 * Jakarta Mail {@code Message} beyond the monitor.
 *
 * @param messageId the RFC 822 Message-ID header (stable per message)
 * @param from       sender address
 * @param to         recipient addresses
 * @param subject    subject line (never null; empty string if absent)
 * @param body       best-effort plain-text body
 * @param receivedAt when the mail server received the message
 */
public record IncomingEmail(
        String messageId,
        String from,
        List<String> to,
        String subject,
        String body,
        Instant receivedAt
) {
}
