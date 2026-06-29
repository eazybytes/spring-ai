package com.eazybytes.support.agent.service;

import com.eazybytes.support.agent.model.IncomingEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link EmailHandler} that simply logs whatever lands in the inbox.
 * <p>
 * It is the baseline so the monitoring loop is verifiable on its own. Replace
 * or wrap it once the agent's real actions (classification, LLM reply drafting,
 * tool calls, ...) are wired in.
 */
@Component
public class LoggingEmailHandler implements EmailHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailHandler.class);

    @Override
    public boolean handle(IncomingEmail email) {
        log.info("""
                === New email ===
                From    : {}
                To      : {}
                Subject : {}
                Body    :
                {}
                =================""",
                email.from(), email.to(), email.subject(), email.body());
        return true;
    }
}
