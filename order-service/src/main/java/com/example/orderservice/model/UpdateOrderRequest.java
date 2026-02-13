package com.example.orderservice.model;

import java.math.BigDecimal;

public record UpdateOrderRequest(
        String customerName,
        String product,
        Integer quantity,
        BigDecimal price,
        OrderStatus status
) {
}
