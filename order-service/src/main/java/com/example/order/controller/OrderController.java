package com.example.order.controller;

import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
        return orderService.createOrder(createOrderRequest);
    }
}
