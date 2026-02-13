package com.example.orderservice.model;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String customerName,
        String product,
        int quantity,
        BigDecimal price
) {
}
