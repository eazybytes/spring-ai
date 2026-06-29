package com.eazybytes.mcp.server.tool;

import com.eazybytes.mcp.server.domain.Customer;
import com.eazybytes.mcp.server.domain.CustomerOrder;
import com.eazybytes.mcp.server.domain.Enums.PaymentStatus;
import com.eazybytes.mcp.server.domain.OrderItem;
import com.eazybytes.mcp.server.domain.Payment;
import com.eazybytes.mcp.server.domain.Product;
import com.eazybytes.mcp.server.domain.SupportTicket;
import com.eazybytes.mcp.server.dto.SupportDtos.CustomerInfo;
import com.eazybytes.mcp.server.dto.SupportDtos.DuplicateChargeResult;
import com.eazybytes.mcp.server.dto.SupportDtos.OrderDetails;
import com.eazybytes.mcp.server.dto.SupportDtos.OrderItemInfo;
import com.eazybytes.mcp.server.dto.SupportDtos.PaymentInfo;
import com.eazybytes.mcp.server.dto.SupportDtos.ProductInfo;
import com.eazybytes.mcp.server.dto.SupportDtos.TicketHistory;
import com.eazybytes.mcp.server.dto.SupportDtos.TicketInfo;
import com.eazybytes.mcp.server.dto.SupportDtos.WarrantyStatus;
import com.eazybytes.mcp.server.repository.CustomerRepository;
import com.eazybytes.mcp.server.repository.OrderRepository;
import com.eazybytes.mcp.server.repository.PaymentRepository;
import com.eazybytes.mcp.server.repository.ProductRepository;
import com.eazybytes.mcp.server.repository.SupportTicketRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Read-only MCP tools: everything the agent uses to <em>understand</em> an email
 * before it acts — identify the customer, pull orders and products, detect a
 * duplicate charge, check a warranty window, and review past tickets for repeat
 * failures. No method here mutates state.
 */
@Service
@Transactional(readOnly = true)
public class SupportQueryTools {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final SupportTicketRepository tickets;

    public SupportQueryTools(CustomerRepository customers,
                             ProductRepository products,
                             OrderRepository orders,
                             PaymentRepository payments,
                             SupportTicketRepository tickets) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.tickets = tickets;
    }

    // ---------------------------------------------------------------------
    // Identify the customer
    // ---------------------------------------------------------------------

    @McpTool(name = "lookup_customer_by_email",
            description = "Look up a customer account by their email address. Returns name, "
                    + "contact details, preferred language, and loyalty tier. Use this first to "
                    + "identify who sent the email and how to address them.")
    public CustomerInfo lookupCustomerByEmail(
            @McpToolParam(description = "The customer's email address, e.g. sarah.mitchell@example.com")
            String email) {
        Customer c = customers.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No customer account found for email: " + email));
        return toCustomerInfo(c);
    }

    // ---------------------------------------------------------------------
    // Pull the customer's orders
    // ---------------------------------------------------------------------

    @McpTool(name = "get_customer_orders",
            description = "List all orders placed by a customer (most recent first), each with "
                    + "its line items and payments. Use this to find the order an email is about, "
                    + "or to see the customer's purchase history.")
    public List<OrderDetails> getCustomerOrders(
            @McpToolParam(description = "The customer's email address")
            String email) {
        return orders.findByCustomerEmailIgnoreCaseOrderByOrderDateDesc(email.trim())
                .stream()
                .map(this::toOrderDetails)
                .toList();
    }

    @McpTool(name = "get_order_by_number",
            description = "Fetch a single order by its order number (the reference customers "
                    + "quote, e.g. \"#4471\"), including line items and every payment charged "
                    + "against it.")
    public OrderDetails getOrderByNumber(
            @McpToolParam(description = "The order number exactly as referenced, digits only, e.g. 4471")
            String orderNumber) {
        return toOrderDetails(requireOrder(orderNumber));
    }

    // ---------------------------------------------------------------------
    // Products / pre-sales
    // ---------------------------------------------------------------------

    @McpTool(name = "search_products",
            description = "Search the product catalog by name or SKU fragment. Use for pre-sales "
                    + "questions or to identify which product a customer is describing.")
    public List<ProductInfo> searchProducts(
            @McpToolParam(description = "A product name or SKU fragment, e.g. \"X200\" or \"blender\"")
            String query) {
        String q = query.trim();
        return products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(q, q)
                .stream()
                .map(this::toProductInfo)
                .toList();
    }

    @McpTool(name = "get_product_by_sku",
            description = "Get full details for one product by its SKU, including the "
                    + "specifications JSON (voltage, dimensions, materials, etc.). Use this to "
                    + "answer spec questions such as whether a product supports European voltage.")
    public ProductInfo getProductBySku(
            @McpToolParam(description = "The product SKU, e.g. X200 or BLND-300")
            String sku) {
        Product p = products.findBySkuIgnoreCase(sku.trim())
                .orElseThrow(() -> new IllegalArgumentException("No product found with SKU: " + sku));
        return toProductInfo(p);
    }

    // ---------------------------------------------------------------------
    // Billing: duplicate charge detection
    // ---------------------------------------------------------------------

    @McpTool(name = "detect_duplicate_charges",
            description = "Analyse the payments on an order to detect a duplicate/double charge. "
                    + "Compares the total captured against the order total and flags repeated "
                    + "charges of the same amount. Use when a customer says they were charged twice.")
    public DuplicateChargeResult detectDuplicateCharges(
            @McpToolParam(description = "The order number to inspect, e.g. 4471")
            String orderNumber) {
        CustomerOrder order = requireOrder(orderNumber);
        List<Payment> captured = payments.findByOrderOrderNumberOrderByChargedAtAsc(orderNumber.trim())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.CAPTURED)
                .toList();

        BigDecimal expected = order.getTotalAmount();
        BigDecimal totalCharged = captured.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overcharged = totalCharged.subtract(expected).max(BigDecimal.ZERO);
        boolean duplicate = captured.size() > 1 && overcharged.signum() > 0;

        String summary = duplicate
                ? "Duplicate charge detected: order %s was charged %d times totalling %s %s, but the order total is only %s %s — overcharged by %s %s. Refund one charge."
                        .formatted(orderNumber, captured.size(), totalCharged, order.getCurrency(),
                                expected, order.getCurrency(), overcharged, order.getCurrency())
                : "No duplicate detected: order %s has %d captured charge(s) totalling %s %s against an order total of %s %s."
                        .formatted(orderNumber, captured.size(), totalCharged, order.getCurrency(),
                                expected, order.getCurrency());

        return new DuplicateChargeResult(orderNumber, duplicate, captured.size(), totalCharged,
                expected, overcharged, captured.stream().map(this::toPaymentInfo).toList(), summary);
    }

    // ---------------------------------------------------------------------
    // Warranty window
    // ---------------------------------------------------------------------

    @McpTool(name = "check_warranty",
            description = "Check whether a product on an order is still within its warranty "
                    + "window, based on the order date plus the product's warranty length. Use "
                    + "before approving a warranty-based refund or replacement.")
    public WarrantyStatus checkWarranty(
            @McpToolParam(description = "The order number the product was bought on, e.g. 4198")
            String orderNumber,
            @McpToolParam(required = false, description = "The product SKU to check; optional if "
                    + "the order has a single line item")
            String sku) {
        CustomerOrder order = requireOrder(orderNumber);
        OrderItem item = resolveItem(order, sku);
        Product product = item.getProduct();

        LocalDate end = order.getOrderDate().plusMonths(product.getWarrantyMonths());
        boolean inWarranty = !LocalDate.now().isAfter(end);
        String summary = inWarranty
                ? "%s on order %s is IN warranty (purchased %s, %d-month warranty ends %s)."
                        .formatted(product.getName(), orderNumber, order.getOrderDate(),
                                product.getWarrantyMonths(), end)
                : "%s on order %s is OUT of warranty (purchased %s, warranty expired %s)."
                        .formatted(product.getName(), orderNumber, order.getOrderDate(), end);

        return new WarrantyStatus(orderNumber, product.getSku(), product.getName(),
                order.getOrderDate(), product.getWarrantyMonths(), end, inWarranty, summary);
    }

    // ---------------------------------------------------------------------
    // History: repeat-failure detection
    // ---------------------------------------------------------------------

    @McpTool(name = "get_customer_ticket_history",
            description = "Retrieve the customer's past support tickets (most recent first) so "
                    + "you can recognise repeat failures or recurring complaints — e.g. the same "
                    + "product breaking for the third time, which warrants goodwill.")
    public TicketHistory getCustomerTicketHistory(
            @McpToolParam(description = "The customer's email address")
            String email) {
        List<SupportTicket> found =
                tickets.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(email.trim());
        List<TicketInfo> infos = found.stream().map(this::toTicketInfo).toList();
        String summary = "Customer %s has %d prior ticket(s) on record.".formatted(email, found.size());
        return new TicketHistory(email, found.size(), infos, summary);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private CustomerOrder requireOrder(String orderNumber) {
        return orders.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("No order found with number: " + orderNumber));
    }

    private OrderItem resolveItem(CustomerOrder order, String sku) {
        List<OrderItem> items = order.getItems();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order " + order.getOrderNumber() + " has no line items.");
        }
        if (sku == null || sku.isBlank()) {
            if (items.size() > 1) {
                throw new IllegalArgumentException("Order " + order.getOrderNumber()
                        + " has multiple items; specify a SKU to check warranty.");
            }
            return items.get(0);
        }
        return items.stream()
                .filter(i -> i.getProduct().getSku().equalsIgnoreCase(sku.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order " + order.getOrderNumber() + " does not contain SKU " + sku));
    }

    private CustomerInfo toCustomerInfo(Customer c) {
        return new CustomerInfo(c.getId(), c.getFullName(), c.getEmail(), c.getPhone(),
                c.getPreferredLanguage(), c.getLoyaltyTier().name());
    }

    private ProductInfo toProductInfo(Product p) {
        return new ProductInfo(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getCategory(), p.getPrice(), p.getCurrency(), p.getSpecifications(),
                p.getWarrantyMonths(), p.getStockQuantity());
    }

    private OrderDetails toOrderDetails(CustomerOrder o) {
        List<OrderItemInfo> items = o.getItems().stream()
                .map(i -> new OrderItemInfo(i.getProduct().getSku(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitPrice()))
                .toList();
        List<PaymentInfo> pays = o.getPayments().stream().map(this::toPaymentInfo).toList();
        Customer c = o.getCustomer();
        return new OrderDetails(o.getOrderNumber(), c.getFullName(), c.getEmail(), o.getOrderDate(),
                o.getStatus().name(), o.getShippingAddress(), o.getTotalAmount(), o.getCurrency(),
                items, pays);
    }

    private PaymentInfo toPaymentInfo(Payment p) {
        return new PaymentInfo(p.getId(), p.getAmount(), p.getCurrency(), p.getPaymentMethod(),
                p.getTransactionRef(), p.getStatus().name(), p.getChargedAt());
    }

    private TicketInfo toTicketInfo(SupportTicket t) {
        return new TicketInfo(t.getId(), t.getSubject(), t.getIntent().name(),
                t.getSentiment().name(), t.getStatus().name(), t.getResolution(), t.getCreatedAt());
    }
}
