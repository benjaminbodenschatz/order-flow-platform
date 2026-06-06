package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order) {
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE orders
                SET status = ?,
                    customer_id = ?,
                    product_id = ?,
                    quantity = ?,
                    created_at = ?,
                    updated_at = ?
                WHERE order_id = ?
                """,
                order.status().name(),
                order.customerId(),
                order.productId(),
                order.quantity(),
                Timestamp.from(order.createdAt()),
                Timestamp.from(order.updatedAt()),
                order.orderId()
        );

        if (updatedRows == 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO orders (
                         order_id,
                         status,
                         customer_id,
                         product_id,
                         quantity,
                         created_at,
                         updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    order.orderId(),
                    order.status().name(),
                    order.customerId(),
                    order.productId(),
                    order.quantity(),
                    Timestamp.from(order.createdAt()),
                    Timestamp.from(order.updatedAt())
            );
        }

        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        List<Order> orders = jdbcTemplate.query(
                """
                SELECT order_id,
                      status,
                      customer_id,
                      product_id,
                      quantity,
                      created_at,
                      updated_at
                FROM orders
                WHERE order_id = ?
                """,
                this::mapRowToOrder,
                orderId
        );

        return orders.stream().findFirst();
    }

    @Override
    public List<Order> findAll() {
        return jdbcTemplate.query(
                """
                SELECT order_id,
                      status,
                      customer_id,
                      product_id,
                      quantity,
                      created_at,
                      updated_at
                FROM orders
                ORDER BY created_at, order_id
                """,
                this::mapRowToOrder
        );
    }

    @Override
    public List<Order> findAllByStatus(OrderStatus status) {
        return jdbcTemplate.query(
                """
                SELECT order_id,
                      status,
                      customer_id,
                      product_id,
                      quantity,
                      created_at,
                      updated_at
                FROM orders
                WHERE status = ?
                ORDER BY created_at, order_id
                """,
                this::mapRowToOrder,
                status.name()
        );
    }

    @Override
    public List<Order> findAll(int limit, int offset) {
        return jdbcTemplate.query(
                """
                SELECT order_id,
                      status,
                      customer_id,
                      product_id,
                      quantity,
                      created_at,
                      updated_at
                FROM orders
                ORDER BY created_at, order_id
                LIMIT ?
                OFFSET ?
                """,
                this::mapRowToOrder,
                limit,
                offset
        );
    }

    @Override
    public List<Order> findAllByStatus(OrderStatus status, int limit, int offset) {
        return jdbcTemplate.query(
                """
                SELECT order_id,
                      status,
                      customer_id,
                      product_id,
                      quantity,
                      created_at,
                      updated_at
                FROM orders
                WHERE status = ?
                ORDER BY created_at, order_id
                LIMIT ?
                OFFSET ?
                """,
                this::mapRowToOrder,
                status.name(),
                limit,
                offset
        );
    }

    @Override
    public long countAll() {
       Long count = jdbcTemplate.queryForObject(
               """
               SELECT COUNT(*)
               FROM orders
               """,
               Long.class
       );

       return count != null ? count : 0;
    }

    @Override
    public long countByStatus(OrderStatus status) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM orders
                WHERE status = ?
                """,
                Long.class,
                status.name()
        );

        return count != null ? count : 0;
    }

    private Order mapRowToOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Order(
                resultSet.getString("order_id"),
                OrderStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("customer_id"),
                resultSet.getString("product_id"),
                resultSet.getInt("quantity"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

}
