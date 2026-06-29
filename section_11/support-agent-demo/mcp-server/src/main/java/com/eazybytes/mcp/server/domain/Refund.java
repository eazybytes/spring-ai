package com.eazybytes.mcp.server.domain;

import com.eazybytes.mcp.server.domain.Enums.RefundStatus;
import com.eazybytes.mcp.server.domain.Enums.RefundType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private BigDecimal amount;
    private String currency;
    private String reason;

    @Enumerated(EnumType.STRING)
    private RefundType refundType;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRefundType(RefundType refundType) {
        this.refundType = refundType;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public RefundType getRefundType() {
        return refundType;
    }

    public RefundStatus getStatus() {
        return status;
    }
}
