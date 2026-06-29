package com.eazybytes.support.agent.service;

import com.eazybytes.support.agent.model.IncomingEmail;

/**
 * Strategy for reacting to a newly discovered email.
 * <p>
 * The {@link InboxMonitor} only knows how to <em>find</em> new mail; deciding
 * what to <em>do</em> with it (classify, draft a reply, open a ticket, hand it
 * to an LLM, ...) is delegated here. Provide your own {@code @Component}
 * implementation to plug the agent's behaviour in.
 */
@FunctionalInterface
public interface EmailHandler {

    /**
     * Handle a single new email.
     *
     * @return {@code true} if the email was processed successfully and may be
     *         marked as read; {@code false} to leave it unread for a retry on
     *         the next poll.
     */
    boolean handle(IncomingEmail email);
}
