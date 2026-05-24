package com.example.order.error;

import com.example.order.domain.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(String orderId, OrderStatus currentStatus, String action) {
        super("Cannot " + action + " order " + orderId + " when status is " + currentStatus);
    }
}
