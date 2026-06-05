package com.example.order.error;

import com.example.order.domain.OrderStatus;

import java.util.Arrays;
import java.util.stream.Collectors;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(String status) {
        super("Invalid order status: " + status + ". Allowed values: " + allowedValues());
    }

    private static String allowedValues() {
        return Arrays.stream(OrderStatus.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
