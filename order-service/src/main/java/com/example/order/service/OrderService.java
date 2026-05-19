package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                OrderStatus.CREATED,
                createOrderRequest.customerId(),
                createOrderRequest.productId(),
                createOrderRequest.quantity(),
                Instant.now()
        );

        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return toOrderResponse(order);
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.orderId(),
                order.status(),
                order.customerId(),
                order.productId(),
                order.quantity(),
                order.createdAt()
        );
    }
}