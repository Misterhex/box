package com.example.orderservice.controller;

import com.example.orderservice.model.CreateOrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.UpdateOrderRequest;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.OrderService.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Order>> createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request)
                .thenApply(order -> ResponseEntity.status(HttpStatus.CREATED).body(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderService.getOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Collection<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<Order>> updateOrder(@PathVariable String id,
                                                                @RequestBody UpdateOrderRequest request) {
        return orderService.updateOrder(id, request)
                .thenApply(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<Void>> deleteOrder(@PathVariable String id) {
        return orderService.deleteOrder(id)
                .thenApply(v -> ResponseEntity.noContent().<Void>build());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(OrderNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
