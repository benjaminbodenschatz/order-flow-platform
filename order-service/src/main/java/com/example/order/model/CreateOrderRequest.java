package com.example.order.model;

public record CreateOrderRequest (
        String customerId,
        String productId,
        int quantity
) {
}