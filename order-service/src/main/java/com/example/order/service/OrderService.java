package com.example.order.service;

import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        return new OrderResponse(
                UUID.randomUUID().toString(),
                "CREATED",
                createOrderRequest.customerId(),
                createOrderRequest.productId(),
                createOrderRequest.quantity()
        );
    }
}