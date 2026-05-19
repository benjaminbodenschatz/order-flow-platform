package com.example.order.error;

public class OrderNotFoundException extends RuntimeException{

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
