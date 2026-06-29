-- =============================================================================
-- Support Agent — schema
--
-- utf8mb4 everywhere so raw customer emails (emoji, accents, Hindi/Devanagari,
-- etc.) are stored losslessly. The agent reads these tables via the MCP server
-- to: identify the customer & product, pull orders, check the warranty window,
-- detect duplicate charges, recognise repeat failures, and take a resolution.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS mydatabase
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mydatabase;

-- ---------------------------------------------------------------------------
-- Customers
-- ---------------------------------------------------------------------------
CREATE TABLE customers (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    full_name          VARCHAR(150) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(40),
    -- ISO 639-1 hint (en, hi, ...). Lets the agent reply in the customer's
    -- language even when one email mixes two (e.g. half English, half Hindi).
    preferred_language VARCHAR(8)   NOT NULL DEFAULT 'en',
    -- Drives goodwill decisions: a GOLD/PLATINUM customer with a repeat
    -- failure is squarely in "just refund and apologise" territory.
    loyalty_tier       ENUM('STANDARD','SILVER','GOLD','PLATINUM')
                                    NOT NULL DEFAULT 'STANDARD',
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_customers_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------------
CREATE TABLE products (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    sku             VARCHAR(40)    NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    description     TEXT,
    category        VARCHAR(80),
    price           DECIMAL(10,2)  NOT NULL,
    currency        CHAR(3)        NOT NULL DEFAULT 'USD',
    -- Product-agnostic attribute bag (JSON). Holds whatever matters for THIS
    -- product — voltage for an appliance, page count for a book, size for
    -- apparel, etc. Pre-sales questions ("will the X200 run on European
    -- voltage?") are answered straight from here, no order needed, and the
    -- table stays neutral to any product category.
    specifications  JSON,
    -- Warranty length so the agent can test "is this still in the window?"
    -- (0 = no warranty, e.g. books / consumables.)
    warranty_months INT            NOT NULL DEFAULT 12,
    stock_quantity  INT            NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Orders
-- ---------------------------------------------------------------------------
CREATE TABLE orders (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    -- Human-facing reference customers quote in emails ("order #4471").
    order_number     VARCHAR(40)   NOT NULL,
    customer_id      BIGINT        NOT NULL,
    order_date       DATE          NOT NULL,
    status           ENUM('PENDING','PAID','SHIPPED','DELIVERED',
                          'CANCELLED','RETURNED')
                                   NOT NULL DEFAULT 'PENDING',
    shipping_address VARCHAR(400),
    total_amount     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency         CHAR(3)       NOT NULL DEFAULT 'USD',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_orders_number (order_number),
    KEY idx_orders_customer (customer_id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Order line items (an order can contain several products)
-- ---------------------------------------------------------------------------
CREATE TABLE order_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_items_order (order_id),
    KEY idx_items_product (product_id),
    CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id)   ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Payments / charges
-- One order can have MORE THAN ONE captured payment row — that is precisely
-- how the agent spots a duplicate charge ("charged twice for order #4471").
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    order_id        BIGINT        NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    currency        CHAR(3)       NOT NULL DEFAULT 'USD',
    payment_method  VARCHAR(40)   NOT NULL DEFAULT 'CARD',
    transaction_ref VARCHAR(80)   NOT NULL,
    status          ENUM('AUTHORIZED','CAPTURED','FAILED',
                         'REFUNDED','PARTIALLY_REFUNDED')
                                  NOT NULL DEFAULT 'CAPTURED',
    charged_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payments_txn (transaction_ref),
    KEY idx_payments_order (order_id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Refunds — the resolution the agent writes back when it takes action
-- ---------------------------------------------------------------------------
CREATE TABLE refunds (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    order_id    BIGINT        NOT NULL,
    payment_id  BIGINT,                       -- which charge is being reversed
    amount      DECIMAL(10,2) NOT NULL,
    currency    CHAR(3)       NOT NULL DEFAULT 'USD',
    reason      VARCHAR(400),
    -- Why the money is going back — useful for reporting & policy checks.
    refund_type ENUM('GOODWILL','DUPLICATE_CHARGE','WARRANTY',
                     'RETURN','OTHER')        NOT NULL DEFAULT 'OTHER',
    status      ENUM('REQUESTED','APPROVED','PROCESSED','REJECTED')
                              NOT NULL DEFAULT 'PROCESSED',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_refunds_order (order_id),
    CONSTRAINT fk_refunds_order   FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Support tickets — log of every inbound email + what the agent did.
-- Doubles as the HISTORY the agent reads to recognise a repeat failure
-- ("cracked jug — third time this has happened").
-- ---------------------------------------------------------------------------
CREATE TABLE support_tickets (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id       BIGINT      NOT NULL,
    order_id          BIGINT,                 -- nullable: pre-sales has no order
    product_id        BIGINT,                 -- nullable: not always product-specific
    channel           ENUM('EMAIL','CHAT','PHONE') NOT NULL DEFAULT 'EMAIL',
    subject           VARCHAR(255),
    raw_message       TEXT,                   -- the customer's words, verbatim
    detected_language VARCHAR(20),            -- e.g. 'en', 'hi', 'en+hi'
    -- What the agent decided the email is about.
    intent            ENUM('REFUND_REQUEST','PRESALES_QUESTION','BILLING_ISSUE',
                           'WARRANTY_CLAIM','COMPLAINT','GENERAL','OTHER')
                                  NOT NULL DEFAULT 'OTHER',
    sentiment         ENUM('POSITIVE','NEUTRAL','NEGATIVE','ANGRY')
                                  NOT NULL DEFAULT 'NEUTRAL',
    status            ENUM('OPEN','RESOLVED','ESCALATED')
                                  NOT NULL DEFAULT 'OPEN',
    resolution        TEXT,                   -- what was done / the reply summary
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP   NULL,
    PRIMARY KEY (id),
    KEY idx_tickets_customer (customer_id),
    KEY idx_tickets_product (product_id),
    CONSTRAINT fk_tickets_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_tickets_order    FOREIGN KEY (order_id)    REFERENCES orders (id),
    CONSTRAINT fk_tickets_product  FOREIGN KEY (product_id)  REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
