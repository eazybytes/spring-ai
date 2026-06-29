package com.eazybytes.support.agent.client;

import com.eazybytes.support.agent.client.dto.MailpitMessage;
import com.eazybytes.support.agent.client.dto.MailpitMessageSummary;
import com.eazybytes.support.agent.client.dto.MailpitMessagesResponse;
import com.eazybytes.support.agent.config.InboxProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Thin wrapper over the Mailpit REST API. Encapsulates the few calls the agent
 * needs so the rest of the code never deals with raw HTTP or Mailpit URLs.
 *
 * @see <a href="https://mailpit.axllent.org/docs/api-v1/">Mailpit API v1</a>
 */
@Component
public class MailpitClient {

    private final RestClient rest;
    private final String inboxAddress;

    public MailpitClient(RestClient.Builder builder, InboxProperties props) {
        this.rest = builder.baseUrl(props.baseUrl()).build();
        this.inboxAddress = props.address();
    }

    /**
     * List the unread messages addressed to the support mailbox (newest first).
     * <p>
     * The query is scoped to mail sent <em>to</em> the support address and
     * <em>not from</em> it, so the agent's own outbound replies — which Mailpit
     * also captures — are never picked back up and reprocessed.
     */
    public List<MailpitMessageSummary> listUnread(int limit) {
        String query = "is:unread to:%s !from:%s".formatted(inboxAddress, inboxAddress);
        MailpitMessagesResponse response = rest.get()
                .uri(uri -> uri.path("/api/v1/search")
                        .queryParam("query", query)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(MailpitMessagesResponse.class);
        return response != null ? response.messages() : List.of();
    }

    /**
     * Fetch a full message by id. Side effect: Mailpit marks the message as read.
     */
    public MailpitMessage getMessage(String id) {
        return rest.get()
                .uri("/api/v1/message/{id}", id)
                .retrieve()
                .body(MailpitMessage.class);
    }

    /**
     * Set the read flag for a single message. Used to put a message back to
     * unread when processing fails, so it is retried on the next poll.
     */
    public void setRead(String id, boolean read) {
        rest.put()
                .uri("/api/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReadUpdate(List.of(id), read))
                .retrieve()
                .toBodilessEntity();
    }

    /** Request body for {@code PUT /api/v1/messages}. */
    private record ReadUpdate(
            @JsonProperty("IDs") List<String> ids,
            @JsonProperty("Read") boolean read
    ) {
    }
}
