package com.eazybytes.mcp.server.tool;

import com.eazybytes.mcp.server.domain.Customer;
import com.eazybytes.mcp.server.domain.CustomerOrder;
import com.eazybytes.mcp.server.domain.Enums.Channel;
import com.eazybytes.mcp.server.domain.Enums.Intent;
import com.eazybytes.mcp.server.domain.Enums.PaymentStatus;
import com.eazybytes.mcp.server.domain.Enums.RefundStatus;
import com.eazybytes.mcp.server.domain.Enums.RefundType;
import com.eazybytes.mcp.server.domain.Enums.Sentiment;
import com.eazybytes.mcp.server.domain.Enums.TicketStatus;
import com.eazybytes.mcp.server.domain.Payment;
import com.eazybytes.mcp.server.domain.Refund;
import com.eazybytes.mcp.server.domain.SupportTicket;
import com.eazybytes.mcp.server.dto.SupportDtos.RefundResult;
import com.eazybytes.mcp.server.dto.SupportDtos.TicketLogResult;
import com.eazybytes.mcp.server.repository.CustomerRepository;
import com.eazybytes.mcp.server.repository.OrderRepository;
import com.eazybytes.mcp.server.repository.PaymentRepository;
import com.eazybytes.mcp.server.repository.ProductRepository;
import com.eazybytes.mcp.server.repository.RefundRepository;
import com.eazybytes.mcp.server.repository.SupportTicketRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * State-changing MCP tools: the actions the agent <em>takes</em> once it has
 * decided on a resolution — issue a refund (and mark the reversed payment) and
 * log the interaction as a support ticket. Every method here writes to the DB.
 */
@Service
@Transactional
public class SupportActionTools {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final SupportTicketRepository tickets;

    public SupportActionTools(CustomerRepository customers,
                              ProductRepository products,
                              OrderRepository orders,
                              PaymentRepository payments,
                              RefundRepository refunds,
                              SupportTicketRepository tickets) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.tickets = tickets;
    }

    // ---------------------------------------------------------------------
    // Take action: issue a refund
    // ---------------------------------------------------------------------

    @McpTool(name = "issue_refund",
            description = "Issue a refund against an order and record it. Optionally tie it to a "
                    + "specific charge by transaction reference (e.g. when reversing one half of "
                    + "a duplicate charge), which marks that payment as REFUNDED. This takes real "
                    + "action — only call once you have decided a refund is warranted.")
    public RefundResult issueRefund(
            @McpToolParam(description = "The order number to refund against, e.g. 4471")
            String orderNumber,
            @McpToolParam(description = "The amount to refund, e.g. 199.99")
            BigDecimal amount,
            @McpToolParam(description = "Why the money is going back")
            RefundType refundType,
            @McpToolParam(description = "A short human-readable reason for the refund")
            String reason,
            @McpToolParam(required = false, description = "Transaction reference of the specific "
                    + "charge being reversed; optional. If given, that payment is marked REFUNDED.")
            String transactionRef) {
        CustomerOrder order = requireOrder(orderNumber);

        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setAmount(amount);
        refund.setCurrency(order.getCurrency());
        refund.setReason(reason);
        refund.setRefundType(refundType);
        refund.setStatus(RefundStatus.PROCESSED);

        if (transactionRef != null && !transactionRef.isBlank()) {
            Payment payment = payments.findByTransactionRef(transactionRef.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No payment found with transaction reference: " + transactionRef));
            payment.setStatus(PaymentStatus.REFUNDED);
            refund.setPayment(payment);
        }

        Refund saved = refunds.save(refund);
        String summary = "Refund of %s %s processed on order %s (%s): %s"
                .formatted(amount, order.getCurrency(), orderNumber, refundType, reason);
        return new RefundResult(saved.getId(), orderNumber, amount, order.getCurrency(),
                refundType.name(), saved.getStatus().name(), summary);
    }

    // ---------------------------------------------------------------------
    // Record what happened: log the ticket
    // ---------------------------------------------------------------------

    @McpTool(name = "log_support_ticket",
            description = "Record this email interaction as a support ticket, capturing the raw "
                    + "message, detected language, classified intent/sentiment, and the "
                    + "resolution. Call this last to log what was decided and done.")
    public TicketLogResult logSupportTicket(
            @McpToolParam(description = "Email of the customer the ticket is for")
            String customerEmail,
            @McpToolParam(description = "The customer's original message, verbatim")
            String rawMessage,
            @McpToolParam(description = "What the email is about")
            Intent intent,
            @McpToolParam(description = "The customer's tone")
            Sentiment sentiment,
            @McpToolParam(description = "Short subject line summarising the email")
            String subject,
            @McpToolParam(description = "Detected language code(s), e.g. en, hi, or en+hi")
            String detectedLanguage,
            @McpToolParam(description = "What was decided/done and the gist of the reply sent back")
            String resolution,
            @McpToolParam(required = false, description = "Related order number, if any")
            String orderNumber,
            @McpToolParam(required = false, description = "Related product SKU, if any")
            String sku) {
        Customer customer = customers.findByEmailIgnoreCase(customerEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No customer account found for email: " + customerEmail));

        SupportTicket ticket = new SupportTicket();
        ticket.setCustomer(customer);
        ticket.setChannel(Channel.EMAIL);
        ticket.setRawMessage(rawMessage);
        ticket.setIntent(intent);
        ticket.setSentiment(sentiment);
        ticket.setSubject(subject);
        ticket.setDetectedLanguage(detectedLanguage);
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());

        if (orderNumber != null && !orderNumber.isBlank()) {
            orders.findByOrderNumber(orderNumber.trim()).ifPresent(ticket::setOrder);
        }
        if (sku != null && !sku.isBlank()) {
            products.findBySkuIgnoreCase(sku.trim()).ifPresent(ticket::setProduct);
        }

        SupportTicket saved = tickets.save(ticket);
        return new TicketLogResult(saved.getId(), saved.getStatus().name(),
                "Logged ticket #%d for %s.".formatted(saved.getId(), customerEmail));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private CustomerOrder requireOrder(String orderNumber) {
        return orders.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("No order found with number: " + orderNumber));
    }
}
