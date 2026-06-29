package com.eazybytes.support.agent.service;

import com.eazybytes.support.agent.client.MailpitClient;
import com.eazybytes.support.agent.client.dto.MailpitAddress;
import com.eazybytes.support.agent.client.dto.MailpitMessage;
import com.eazybytes.support.agent.client.dto.MailpitMessageSummary;
import com.eazybytes.support.agent.config.InboxProperties;
import com.eazybytes.support.agent.model.IncomingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Polls the Mailpit inbox on a fixed interval, turns every unread message into
 * an {@link IncomingEmail}, and forwards it to the configured
 * {@link EmailHandler}.
 * <p>
 * Read/unread state lives on the Mailpit server: fetching a message marks it
 * read, which is how we avoid reprocessing. If the handler reports failure, the
 * message is flipped back to unread so the next poll retries it.
 */
@Service
public class InboxMonitor {

    private static final Logger log = LoggerFactory.getLogger(InboxMonitor.class);

    private final MailpitClient mailpit;
    private final EmailHandler handler;
    private final InboxProperties props;

    public InboxMonitor(MailpitClient mailpit, EmailHandler handler, InboxProperties props) {
        this.mailpit = mailpit;
        this.handler = handler;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${support-agent.inbox.poll-interval:10000}")
    public void poll() {
        try {
            List<MailpitMessageSummary> unread = mailpit.listUnread(props.batchSize());
            if (unread.isEmpty()) {
                log.debug("No new mail");
                return;
            }
            log.info("Found {} new message(s)", unread.size());
            for (MailpitMessageSummary summary : unread) {
                processOne(summary.id());
            }
        } catch (Exception e) {
            // Mailpit may be starting up or briefly unreachable. Log and let the
            // next scheduled poll retry rather than killing the scheduler.
            log.warn("Inbox poll failed: {}", e.getMessage(), e);
        }
    }

    private void processOne(String id) {
        try {
            // Fetching the full message marks it read on the server.
            MailpitMessage message = mailpit.getMessage(id);
            IncomingEmail email = toIncomingEmail(message);
            boolean handled = handler.handle(email);
            if (!handled) {
                // Leave it unread so the next poll picks it up again.
                mailpit.setRead(id, false);
            }
        } catch (Exception e) {
            log.error("Failed to process message {}; resetting to unread for retry", id, e);
            try {
                mailpit.setRead(id, false);
            } catch (Exception reset) {
                log.warn("Could not reset message {} to unread: {}", id, reset.getMessage());
            }
        }
    }

    private IncomingEmail toIncomingEmail(MailpitMessage message) {
        String from = message.from() != null ? message.from().address() : "(unknown)";
        List<String> to = message.to().stream()
                .map(MailpitAddress::address)
                .toList();
        String subject = message.subject() != null ? message.subject() : "";
        String body = bestBody(message);
        Instant receivedAt = parseDate(message.date());
        return new IncomingEmail(message.messageId(), from, to, subject, body, receivedAt);
    }

    /** Prefer the plain-text body; fall back to HTML if that is all there is. */
    private String bestBody(MailpitMessage message) {
        if (message.text() != null && !message.text().isBlank()) {
            return message.text().strip();
        }
        return message.html() != null ? message.html().strip() : "";
    }

    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(date).toInstant();
        } catch (Exception e) {
            try {
                return Instant.parse(date);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}
