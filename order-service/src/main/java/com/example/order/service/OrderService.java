package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.error.InvalidOrderStateException;
import com.example.order.error.InvalidOrderStatusException;
import com.example.order.error.InvalidPaginationException;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.model.PageResponse;
import com.example.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        Instant now = Instant.now();

        Order order = new Order(
                UUID.randomUUID().toString(),
                OrderStatus.CREATED,
                createOrderRequest.customerId(),
                createOrderRequest.productId(),
                createOrderRequest.quantity(),
                now,
                now
        );

        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return toOrderResponse(order);
    }

    public List<OrderResponse> getAllOrders(String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findAll()
                    .stream()
                    .map(this::toOrderResponse)
                    .toList();
        }

        OrderStatus orderStatus = parseOrderStatus(status);

        return orderRepository.findAllByStatus(orderStatus)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    public PageResponse<OrderResponse> getOrdersPage(String status, int page, int size) {
        validatePagination(page, size);

        int offset = page * size;

        List<Order> orders;
        long totalElements;

        if (status == null || status.isBlank()) {
            orders = orderRepository.findAll(size, offset);
            totalElements = orderRepository.countAll();
        } else {
            OrderStatus orderStatus = parseOrderStatus(status);
            orders = orderRepository.findAllByStatus(orderStatus, size, offset);
            totalElements = orderRepository.countByStatus(orderStatus);
        }

        List<OrderResponse> content = orders.stream()
                .map(this::toOrderResponse)
                .toList();

        int totalPages = calculateTotalPages(totalElements, size);

        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public OrderResponse cancelOrder(String orderId) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (existingOrder.status() != OrderStatus.CREATED) {
           throw new InvalidOrderStateException(orderId, existingOrder.status(), "cancel");
        }

        Order cancelledOrder = new Order(
                existingOrder.orderId(),
                OrderStatus.CANCELLED,
                existingOrder.customerId(),
                existingOrder.productId(),
                existingOrder.quantity(),
                existingOrder.createdAt(),
                Instant.now()
        );

        Order savedOrder = orderRepository.save(cancelledOrder);

        return toOrderResponse(savedOrder);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidPaginationException("Invalid pagination parameter: page must be at least 0");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPaginationException(
                    "Invalid pagination parameter: size must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    private int calculateTotalPages(long totalElements, int size) {
        return (int) Math.ceil((double) totalElements / size);
    }

    private OrderStatus parseOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidOrderStatusException(status);
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.orderId(),
                order.status(),
                order.customerId(),
                order.productId(),
                order.quantity(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}