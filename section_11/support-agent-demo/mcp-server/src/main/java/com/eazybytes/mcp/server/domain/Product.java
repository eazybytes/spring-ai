package com.eazybytes.mcp.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private BigDecimal price;
    private String currency;

    /**
     * Product-agnostic attribute bag stored as native MySQL JSON, surfaced here
     * as the raw JSON string so any product category (voltage, page count,
     * apparel size, ...) flows through untouched.
     */
    @Column(columnDefinition = "json")
    private String specifications;

    private Integer warrantyMonths;
    private Integer stockQuantity;

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSpecifications() {
        return specifications;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }
}
