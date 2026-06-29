package com.eazybytes.support.agent.service;

import com.eazybytes.support.agent.config.InboxProperties;
import com.eazybytes.support.agent.model.AgentResponse;
import com.eazybytes.support.agent.model.IncomingEmail;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Sends the agent's drafted reply back to the customer over SMTP (the same
 * Mailpit instance the inbox is monitored from). The reply is threaded onto the
 * original message via the {@code In-Reply-To}/{@code References} headers so it
 * shows up as a proper response rather than a fresh email.
 */
@Service
public class SupportMailSender {

    private static final Logger log = LoggerFactory.getLogger(SupportMailSender.class);

    /** "On Wed, 11 Jun 2026 at 14:03, ... wrote:" — the usual quoted-reply attribution line. */
    private static final DateTimeFormatter QUOTE_DATE =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    private final JavaMailSender mailSender;
    private final InboxProperties inbox;

    public SupportMailSender(JavaMailSender mailSender, InboxProperties inbox) {
        this.mailSender = mailSender;
        this.inbox = inbox;
    }

    /**
     * Email the agent's reply to whoever sent the original message.
     *
     * @return {@code true} if a reply was sent; {@code false} if there was no
     *         usable recipient or nothing to say.
     */
    public boolean sendReply(IncomingEmail original, AgentResponse response) throws Exception {
        String recipient = original.from();
        if (recipient == null || recipient.isBlank() || "(unknown)".equals(recipient)) {
            log.warn("No usable sender address on email \"{}\"; skipping reply", original.subject());
            return false;
        }
        if (response.replyBody() == null || response.replyBody().isBlank()) {
            log.warn("Agent produced no reply body for email from {}; skipping reply", recipient);
            return false;
        }

        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
        helper.setFrom(inbox.address());
        helper.setTo(recipient);
        helper.setSubject(replySubject(original, response));
        helper.setText(quoteOriginal(original, response.replyBody()));

        // Thread the reply onto the original message when we know its Message-ID.
        String messageId = original.messageId();
        if (messageId != null && !messageId.isBlank()) {
            String ref = messageId.startsWith("<") ? messageId : "<" + messageId + ">";
            mime.setHeader("In-Reply-To", ref);
            mime.setHeader("References", ref);
        }

        mailSender.send(mime);
        log.info("Replied to {} (subject: \"{}\")", recipient, mime.getSubject());
        return true;
    }

    /**
     * Build the reply subject from the original ("Re: <subject>"), which — together
     * with the In-Reply-To/References headers — is what makes mail clients group the
     * reply into the original conversation instead of showing a fresh thread.
     */
    private String replySubject(IncomingEmail original, AgentResponse response) {
        String base = original.subject() != null && !original.subject().isBlank()
                ? original.subject()
                : (response.replySubject() != null ? response.replySubject() : "");
        return base.regionMatches(true, 0, "Re:", 0, 3) ? base : "Re: " + base;
    }

    /**
     * Append the customer's original message under the agent's reply as a quoted
     * block ("On &lt;date&gt;, &lt;sender&gt; wrote: &gt; ..."), the way a normal mail
     * client does, so the reply carries the context it is responding to.
     */
    private String quoteOriginal(IncomingEmail original, String replyBody) {
        String body = original.body() != null ? original.body() : "";
        String quoted = body.isBlank()
                ? ""
                : body.stripTrailing().lines().map(line -> "> " + line).reduce((a, b) -> a + "\n" + b).orElse("");
        return """
                %s

                On %s, %s wrote:
                %s""".formatted(
                replyBody.stripTrailing(),
                QUOTE_DATE.format(original.receivedAt()),
                original.from(),
                quoted);
    }
}
