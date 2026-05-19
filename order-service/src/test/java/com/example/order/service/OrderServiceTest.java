package com.example.order.service;

import com.example.order.domain.OrderStatus;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.repository.InMemoryOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(new InMemoryOrderRepository());
    }

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
        assertEquals(OrderStatus.CREATED, orderResponse.status());
        assertNotNull(orderResponse.createdAt());
    }

    @Test
    void getOrderByIdShouldReturnExistingOrder () {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "customer123",
                "product123",
                3
        );

        OrderResponse createdOrder = orderService.createOrder(createOrderRequest);

        OrderResponse foundOrder = orderService.getOrderById(createdOrder.orderId());

        assertEquals(createdOrder.orderId(), foundOrder.orderId());
        assertEquals(createdOrder.status(), foundOrder.status());
        assertEquals(createdOrder.customerId(), foundOrder.customerId());
        assertEquals(createdOrder.productId(), foundOrder.productId());
        assertEquals(createdOrder.quantity(), foundOrder.quantity());
        assertEquals(createdOrder.createdAt(), foundOrder.createdAt());
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderDoesNotExist() {
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderById("nonexistent-order")
        );
    }
}
