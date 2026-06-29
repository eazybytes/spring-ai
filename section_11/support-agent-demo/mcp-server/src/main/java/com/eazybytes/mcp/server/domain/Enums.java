package com.eazybytes.mcp.server.domain;

/**
 * Domain enumerations mirroring the MySQL ENUM columns. Persisted as STRING so
 * the DB values and the Java names line up one-to-one, and so the MCP tool
 * schemas advertise the exact allowed values to the calling agent.
 */
public final class Enums {

    private Enums() {
    }

    public enum LoyaltyTier {
        STANDARD, SILVER, GOLD, PLATINUM
    }

    public enum OrderStatus {
        PENDING, PAID, SHIPPED, DELIVERED, CANCELLED, RETURNED
    }

    public enum PaymentStatus {
        AUTHORIZED, CAPTURED, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }

    public enum RefundType {
        GOODWILL, DUPLICATE_CHARGE, WARRANTY, RETURN, OTHER
    }

    public enum RefundStatus {
        REQUESTED, APPROVED, PROCESSED, REJECTED
    }

    public enum Channel {
        EMAIL, CHAT, PHONE
    }

    public enum Intent {
        REFUND_REQUEST, PRESALES_QUESTION, BILLING_ISSUE,
        WARRANTY_CLAIM, COMPLAINT, GENERAL, OTHER
    }

    public enum Sentiment {
        POSITIVE, NEUTRAL, NEGATIVE, ANGRY
    }

    public enum TicketStatus {
        OPEN, RESOLVED, ESCALATED
    }
}
