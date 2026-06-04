package com.example.order.model;

import com.example.order.domain.OrderStatus;

import java.time.Instant;

public record OrderResponse(
        String orderId,
        OrderStatus status,
        String customerId,
        String productId,
        int quantity,
        Instant createdAt,
        Instant updatedAt
) {
}