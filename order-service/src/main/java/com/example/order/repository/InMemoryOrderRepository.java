package com.example.order.repository;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        orders.put(order.orderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<Order> findAll() {
        return orders.values()
                .stream()
                .sorted(Comparator.comparing(Order::createdAt).thenComparing(Order::orderId))
                .toList();
    }

    @Override
    public List<Order> findAllByStatus(OrderStatus status) {
        return orders.values()
                .stream()
                .filter(order -> order.status() == status)
                .sorted(Comparator.comparing(Order::createdAt).thenComparing(Order::orderId))
                .toList();
    }

    @Override
    public List<Order> findAll(int limit, int offset) {
        return findAll()
                .stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public List<Order> findAllByStatus(OrderStatus status, int limit, int offset) {
        return findAllByStatus(status)
                .stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long countAll() {
        return orders.size();
    }

    @Override
    public long countByStatus(OrderStatus status) {
        return orders.values()
                .stream()
                .filter(order -> order.status() == status)
                .count();
    }
}
