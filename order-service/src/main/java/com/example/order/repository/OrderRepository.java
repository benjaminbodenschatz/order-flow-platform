package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String orderId);

    List<Order> findAll();

    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findAll(int limit, int offset);

    List<Order> findAllByStatus(OrderStatus status, int limit, int offset);

    long countAll();

    long countByStatus(OrderStatus status);
}
