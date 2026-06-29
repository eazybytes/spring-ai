package com.eazybytes.support.agent.controller;

import com.eazybytes.support.agent.config.InboxProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Test-only helper to drop a fake email into the monitored inbox over SMTP,
 * so the inbox monitor has something to pick up.
 *
 * <pre>
 *   curl -X POST "http://localhost:8080/seed-mail?subject=Refund&amp;body=Where%20is%20my%20order%3F"
 * </pre>
 */
@RestController
public class SeedMailController {

    private final JavaMailSender mailSender;
    private final InboxProperties inbox;

    public SeedMailController(JavaMailSender mailSender, InboxProperties inbox) {
        this.mailSender = mailSender;
        this.inbox = inbox;
    }

    @PostMapping("/seed-mail")
    public ResponseEntity<SeedResult> seed(
            @RequestParam(defaultValue = "customer@example.com") String from,
            @RequestParam(defaultValue = "Test support request") String subject,
            @RequestParam(defaultValue = "Hi, I need help with my recent order.") String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(inbox.address());
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // Usually means the Mailpit SMTP server (compose.yaml) isn't up yet.
            return ResponseEntity.status(502).body(SeedResult.failed(from, inbox.address(), subject, e));
        }

        return ResponseEntity.ok(SeedResult.sent(from, inbox.address(), subject, body));
    }

    /**
     * Outcome of a seeding attempt — echoes back what was injected so the caller
     * can confirm exactly which email the agent is about to pick up.
     *
     * @param status    {@code "sent"} or {@code "failed"}
     * @param message   human-readable description of what happened
     * @param from      the sender address the fake email was sent as
     * @param to        the monitored mailbox it was delivered to
     * @param subject   the email subject
     * @param body      the email body (only present on success)
     * @param error     failure detail (only present on failure)
     * @param timestamp when the attempt was made
     */
    public record SeedResult(
            String status,
            String message,
            String from,
            String to,
            String subject,
            String body,
            String error,
            Instant timestamp
    ) {
        static SeedResult sent(String from, String to, String subject, String body) {
            return new SeedResult("sent",
                    "Email delivered to %s; the agent will pick it up on the next inbox poll.".formatted(to),
                    from, to, subject, body, null, Instant.now());
        }

        static SeedResult failed(String from, String to, String subject, Exception e) {
            return new SeedResult("failed",
                    "Could not deliver email to %s. Is the Mailpit SMTP server running?".formatted(to),
                    from, to, subject, null, e.getMessage(), Instant.now());
        }
    }
}
