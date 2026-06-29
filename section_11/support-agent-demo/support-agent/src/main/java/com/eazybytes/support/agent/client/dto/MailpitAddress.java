package com.eazybytes.support.agent.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An email address as represented in Mailpit API responses.
 */
public record MailpitAddress(
        @JsonProperty("Name") String name,
        @JsonProperty("Address") String address
) {
}
