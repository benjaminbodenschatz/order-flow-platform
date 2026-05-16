package com.example.order.model;

public record OrderResponse(
        String orderId,
        String status,
        String customerId,
        String productId,
        int quantity
) {
}