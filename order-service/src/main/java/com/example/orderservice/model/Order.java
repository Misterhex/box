package com.example.orderservice.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The domain entity stored as the Kafka message value.
 * The order ID is also used as the Kafka message key (for partitioning and log compaction).
 */
public class Order {

    private String id;
    private String customerName;
    private String product;
    private int quantity;
    private BigDecimal price;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;

    public Order() {
    }

    @JsonCreator
    public Order(@JsonProperty("id") String id,
                 @JsonProperty("customerName") String customerName,
                 @JsonProperty("product") String product,
                 @JsonProperty("quantity") int quantity,
                 @JsonProperty("price") BigDecimal price,
                 @JsonProperty("status") OrderStatus status,
                 @JsonProperty("createdAt") Instant createdAt,
                 @JsonProperty("updatedAt") Instant updatedAt,
                 @JsonProperty("deleted") boolean deleted) {
        this.id = id;
        this.customerName = customerName;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
