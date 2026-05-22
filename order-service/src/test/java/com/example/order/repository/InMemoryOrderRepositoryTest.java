package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryOrderRepositoryTest {

    @Test
    void saveShouldStoreOrderSoItCanBeFoundById() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();

        Order order = new Order(
                "order-1",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        Order savedOrder = repository.save(order);

        Optional<Order> foundOrder = repository.findById("order-1");

        assertEquals(order, savedOrder);
        assertEquals(Optional.of(order), foundOrder);
    }

    @Test
    void findByIdShouldReturnEmptyWhenOrderDoesNotExist() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();

        Optional<Order> foundOrder = repository.findById("missing-order");

        assertEquals(Optional.empty(), foundOrder);
    }

    @Test
    void findAllShouldReturnSavedOrdersSortedByCreatedAt() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();

        Order laterOrder = new Order(
                "order-2",
                OrderStatus.CREATED,
                "customer-456",
                "product-789",
                1,
                Instant.parse("2026-05-17T12:05:00Z")
        );

        Order earlierOrder = new Order(
                "order-1",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        repository.save(laterOrder);
        repository.save(earlierOrder);

        List<Order> orders = repository.findAll();

        assertEquals(List.of(earlierOrder, laterOrder), orders);
    }
}