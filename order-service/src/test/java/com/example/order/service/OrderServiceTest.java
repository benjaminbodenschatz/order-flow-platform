package com.example.order.service;

import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    private final OrderService orderService = new OrderService();

    @Test
    void createOrderShouldReturnCreatedOrder() {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "customer123",
                "product123",
                3
        );

        OrderResponse orderResponse = orderService.createOrder(createOrderRequest);
        assertNotNull(orderResponse.orderId());
        assertFalse(orderResponse.orderId().isBlank());
        assertEquals("customer123", orderResponse.customerId());
        assertEquals("product123", orderResponse.productId());
        assertEquals(3, orderResponse.quantity());
        assertEquals("CREATED", orderResponse.status());
    }
}
