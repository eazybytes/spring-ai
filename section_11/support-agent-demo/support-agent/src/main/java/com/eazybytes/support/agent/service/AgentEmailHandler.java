package com.eazybytes.support.agent.service;

import com.eazybytes.support.agent.model.AgentResponse;
import com.eazybytes.support.agent.model.IncomingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The real {@link EmailHandler}: hands every new email to the {@link SupportAgent}
 * so the LLM, backed by the MCP tools, resolves it autonomously.
 * <p>
 * Marked {@link Primary} so the {@code InboxMonitor} wires this in ahead of the
 * baseline {@link LoggingEmailHandler}. If the agent throws, we return {@code false}
 * so the message is left unread and retried on the next poll.
 */
@Component
@Primary
public class AgentEmailHandler implements EmailHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentEmailHandler.class);

    private final SupportAgent agent;
    private final SupportMailSender mailSender;

    public AgentEmailHandler(SupportAgent agent, SupportMailSender mailSender) {
        this.agent = agent;
        this.mailSender = mailSender;
    }

    @Override
    public boolean handle(IncomingEmail email) {
        log.info("Handing email from {} (subject: \"{}\") to the support agent", email.from(), email.subject());
        try {
            AgentResponse response = agent.resolve(email);
            log.info("""
                    === Agent resolution ===
                    From    : {}
                    Subject : {}
                    Outcome :
                    {}
                    ========================""",
                    email.from(), email.subject(), response.operatorSummary());
            // Send the drafted reply back to the customer, threaded onto their email.
            mailSender.sendReply(email, response);
            return true;
        } catch (Exception e) {
            // Anything from a transient LLM/MCP error to a tool failure lands here.
            // Leave the mail unread so the next poll retries it.
            log.error("Agent failed to resolve email from {} (subject: \"{}\"); will retry",
                    email.from(), email.subject(), e);
            return false;
        }
    }
}
