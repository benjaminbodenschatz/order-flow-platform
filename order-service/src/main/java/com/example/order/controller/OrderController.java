package com.example.order.controller;

import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest createOrderRequest) {
        return orderService.createOrder(createOrderRequest);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(@PathVariable String orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable String orderId) {
        return orderService.cancelOrder(orderId);
    }
}
