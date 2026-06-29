package com.eazybytes.mcp.server.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Flat, serialization-safe view objects returned by the MCP tools. Built inside
 * the transactional tool methods so no lazy JPA proxy ever escapes to the agent.
 */
public final class SupportDtos {

    private SupportDtos() {
    }

    public record CustomerInfo(
            Long id,
            String fullName,
            String email,
            String phone,
            String preferredLanguage,
            String loyaltyTier) {
    }

    public record ProductInfo(
            Long id,
            String sku,
            String name,
            String description,
            String category,
            BigDecimal price,
            String currency,
            String specifications,
            Integer warrantyMonths,
            Integer stockQuantity) {
    }

    public record OrderItemInfo(
            String sku,
            String productName,
            Integer quantity,
            BigDecimal unitPrice) {
    }

    public record PaymentInfo(
            Long id,
            BigDecimal amount,
            String currency,
            String method,
            String transactionRef,
            String status,
            LocalDateTime chargedAt) {
    }

    public record OrderDetails(
            String orderNumber,
            String customerName,
            String customerEmail,
            LocalDate orderDate,
            String status,
            String shippingAddress,
            BigDecimal totalAmount,
            String currency,
            List<OrderItemInfo> items,
            List<PaymentInfo> payments) {
    }

    public record DuplicateChargeResult(
            String orderNumber,
            boolean duplicateDetected,
            int chargeCount,
            BigDecimal totalCharged,
            BigDecimal expectedAmount,
            BigDecimal overchargedAmount,
            List<PaymentInfo> charges,
            String summary) {
    }

    public record WarrantyStatus(
            String orderNumber,
            String sku,
            String productName,
            LocalDate orderDate,
            int warrantyMonths,
            LocalDate warrantyEndDate,
            boolean inWarranty,
            String summary) {
    }

    public record TicketInfo(
            Long id,
            String subject,
            String intent,
            String sentiment,
            String status,
            String resolution,
            LocalDateTime createdAt) {
    }

    public record TicketHistory(
            String customerEmail,
            int totalTickets,
            List<TicketInfo> tickets,
            String summary) {
    }

    public record RefundResult(
            Long refundId,
            String orderNumber,
            BigDecimal amount,
            String currency,
            String refundType,
            String status,
            String summary) {
    }

    public record TicketLogResult(
            Long ticketId,
            String status,
            String summary) {
    }
}
