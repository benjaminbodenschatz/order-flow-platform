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
                    created_at = ?
                WHERE order_id = ?
                """,
                order.status().name(),
                order.customerId(),
                order.productId(),
                order.quantity(),
                Timestamp.from(order.createdAt()),
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
                         created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    order.orderId(),
                    order.status().name(),
                    order.customerId(),
                    order.productId(),
                    order.quantity(),
                    Timestamp.from(order.createdAt())
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
                      created_at
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
                      created_at
                FROM orders
                ORDER BY created_at, order_id
                """,
                this::mapRowToOrder
        );
    }

    private Order mapRowToOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Order(
                resultSet.getString("order_id"),
                OrderStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("customer_id"),
                resultSet.getString("product_id"),
                resultSet.getInt("quantity"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

}
