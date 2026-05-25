package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcOrderRepositoryTest {

    private JdbcOrderRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
                """
                CREATE TABLE orders (
                    order_id VARCHAR(36) PRIMARY KEY,
                    status VARCHAR(20) NOT NULL,
                    customer_id VARCHAR(255) NOT NULL,
                    product_id VARCHAR(255) NOT NULL,
                    quantity INTEGER NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """
        );

        repository = new JdbcOrderRepository(jdbcTemplate);
    }

    @Test
    void save_whenOrderDoesNotExist_shouldInsertOrder() {
        Order order = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        repository.save(order);

        Optional<Order> result = repository.findById("order-123");

        assertTrue(result.isPresent());
        assertEquals(order, result.get());
    }

    @Test
    void findById_whenOrderDoesNotExist_shouldReturnEmpty() {
        Optional<Order> result = repository.findById("missing-order");

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_whenOrdersExist_shouldReturnOrdersSortedByCreatedAtThenOrderId() {
        Order laterOrder = new Order(
                "order-003",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:05:00Z")
        );

        Order firstTieBreakerOrder = new Order(
                "order-001",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        Order secondTieBreakerOrder = new Order(
                "order-002",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        repository.save(laterOrder);
        repository.save(secondTieBreakerOrder);
        repository.save(firstTieBreakerOrder);

        List<Order> result = repository.findAll();

        assertEquals(List.of(firstTieBreakerOrder, secondTieBreakerOrder, laterOrder), result);
    }

    @Test
    void save_whenOrderWithSameIdAlreadyExists_shouldReplaceExistingOrder() {
        Order originalOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        Order cancelledOrder = new Order(
                "order-123",
                OrderStatus.CANCELLED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        repository.save(originalOrder);
        repository.save(cancelledOrder);

        Optional<Order> result = repository.findById("order-123");

        assertTrue(result.isPresent());
        assertEquals(cancelledOrder, result.get());
    }
}