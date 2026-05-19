package com.example.order.domain;

import java.time.Instant;

public record Order(
        String orderId,
        OrderStatus status,
        String customerId,
        String productId,
        int quantity,
        Instant createdAt
) {
}
