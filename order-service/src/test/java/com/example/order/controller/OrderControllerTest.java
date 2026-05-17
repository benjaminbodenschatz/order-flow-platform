package com.example.order.controller;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
                "CREATED",
                "customer-123",
                "product-123",
                3
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
                .andExpect(jsonPath("$.quantity").value(3));

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
}
