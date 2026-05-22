package com.example.order.controller;

import com.example.order.domain.OrderStatus;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrderService orderService;

    @Test
    void createOrderShouldReturnCreatedOrder() throws Exception {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "customer-123",
                "product-123",
                3
        );

        OrderResponse orderResponse = new OrderResponse(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-123",
                3,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(orderResponse);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-123"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect((jsonPath("$.createdAt").value("2026-05-17T12:00:00Z")));

        verify(orderService).createOrder(createOrderRequest);
    }

    @Test
    void createOrderShouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "customer-123",
                "",
                0
        );

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'productId' && @.message == 'productId is required')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'quantity' && @.message == 'quantity must be at least 1')]").exists())
        ;

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrderByIdShouldReturnOrder() throws Exception {
        OrderResponse response = new OrderResponse(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-123",
                3,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        when(orderService.getOrderById("order-123"))
                .thenReturn(response);

        mockMvc.perform(get("/orders/order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-123"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-17T12:00:00Z"));

        verify(orderService).getOrderById("order-123");
    }

    @Test
    void getOrderByIdShouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getOrderById("nonexistent-order"))
                .thenThrow(new OrderNotFoundException("nonexistent-order"));

        mockMvc.perform(get("/orders/nonexistent-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Order not found"))
                .andExpect(jsonPath("$.message").value("Order not found: nonexistent-order"))
                .andExpect(jsonPath("$.path").value("/orders/nonexistent-order"));

        verify(orderService).getOrderById("nonexistent-order");
    }

    @Test
    void getAllOrdersShouldReturnOrders() throws Exception {
        OrderResponse firstResponse = new OrderResponse(
                "order-abc",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z")
        );

        OrderResponse secondResponse = new OrderResponse(
                "order-def",
                OrderStatus.CREATED,
                "customer-456",
                "product-789",
                1,
                Instant.parse("2026-05-17T12:05:00Z")
        );

        when(orderService.getAllOrders())
                .thenReturn(List.of(firstResponse, secondResponse));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value("order-abc"))
                .andExpect(jsonPath("$[0].customerId").value("customer-123"))
                .andExpect(jsonPath("$[0].productId").value("product-456"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$[1].orderId").value("order-def"))
                .andExpect(jsonPath("$[1].customerId").value("customer-456"))
                .andExpect(jsonPath("$[1].productId").value("product-789"))
                .andExpect(jsonPath("$[1].quantity").value(1))
                .andExpect(jsonPath("$[1].status").value("CREATED"))
                .andExpect(jsonPath("$[1].createdAt").value("2026-05-17T12:05:00Z"));

        verify(orderService).getAllOrders();
    }
}
