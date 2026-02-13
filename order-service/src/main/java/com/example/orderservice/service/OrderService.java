package com.example.orderservice.service;

import com.example.orderservice.model.CreateOrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.model.UpdateOrderRequest;
import com.example.orderservice.store.OrderKafkaStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderService {

    private final OrderKafkaStore store;

    public OrderService(OrderKafkaStore store) {
        this.store = store;
    }

    public CompletableFuture<Order> createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerName(request.customerName());
        order.setProduct(request.product());
        order.setQuantity(request.quantity());
        order.setPrice(request.price());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setDeleted(false);

        return store.put(order).thenApply(result -> order);
    }

    public Optional<Order> getOrder(String id) {
        return store.get(id);
    }

    public Collection<Order> getAllOrders() {
        return store.getAll();
    }

    public CompletableFuture<Order> updateOrder(String id, UpdateOrderRequest request) {
        Order existing = store.get(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (request.customerName() != null) existing.setCustomerName(request.customerName());
        if (request.product() != null) existing.setProduct(request.product());
        if (request.quantity() != null) existing.setQuantity(request.quantity());
        if (request.price() != null) existing.setPrice(request.price());
        if (request.status() != null) existing.setStatus(request.status());
        existing.setUpdatedAt(Instant.now());

        return store.put(existing).thenApply(result -> existing);
    }

    /**
     * Soft delete: publish the order with deleted=true.
     * Log compaction will eventually remove it, and the store
     * drops it from the local map immediately.
     */
    public CompletableFuture<Void> deleteOrder(String id) {
        Order existing = store.get(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        existing.setDeleted(true);
        existing.setUpdatedAt(Instant.now());

        return store.put(existing).thenApply(result -> null);
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String id) {
            super("Order not found: " + id);
        }
    }
}
