package com.example.order.controller;

import com.example.order.domain.OrderStatus;
import com.example.order.error.InvalidOrderStateException;
import com.example.order.error.InvalidOrderStatusException;
import com.example.order.error.InvalidPaginationException;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.model.PageResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    OrderService orderService;

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-123",
                "product-123",
                3
        );

        OrderResponse response = new OrderResponse(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-123",
                3,
                Instant.parse("2026-05-17T12:00:00Z"),
                Instant.parse("2026-05-17T12:00:00Z")
        );

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-123"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-17T12:00:00Z"));

        verify(orderService).createOrder(request);
    }

    @Test
    void createOrder_whenRequestIsInvalid_shouldReturnBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-123",
                "",
                0
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/orders"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'productId' && @.message == 'productId is required')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'quantity' && @.message == 'quantity must be at least 1')]").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    void getOrderById_whenOrderExists_shouldReturnOrder() throws Exception {
        OrderResponse response = new OrderResponse(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-123",
                3,
                Instant.parse("2026-05-17T12:00:00Z"),
                Instant.parse("2026-05-17T12:00:00Z")
        );

        when(orderService.getOrderById("order-123"))
                .thenReturn(response);

        mockMvc.perform(get("/orders/{orderId}", "order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-123"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.createdAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-17T12:00:00Z"));

        verify(orderService).getOrderById("order-123");
    }

    @Test
    void getOrderById_whenOrderDoesNotExist_shouldReturnNotFound() throws Exception {
        when(orderService.getOrderById("nonexistent-order"))
                .thenThrow(new OrderNotFoundException("nonexistent-order"));

        mockMvc.perform(get("/orders/{orderId}", "nonexistent-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Order not found"))
                .andExpect(jsonPath("$.message").value("Order not found: nonexistent-order"))
                .andExpect(jsonPath("$.path").value("/orders/nonexistent-order"));

        verify(orderService).getOrderById("nonexistent-order");
    }

    @Test
    void getAllOrders_whenOrdersExist_shouldReturnOrders() throws Exception {
        OrderResponse firstResponse = new OrderResponse(
                "order-abc",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z"),
                Instant.parse("2026-05-17T12:00:00Z")
        );

        OrderResponse secondResponse = new OrderResponse(
                "order-def",
                OrderStatus.CREATED,
                "customer-456",
                "product-789",
                1,
                Instant.parse("2026-05-17T12:05:00Z"),
                Instant.parse("2026-05-17T12:05:00Z")
        );

        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(firstResponse, secondResponse),
                0,
                20,
                2,
                1
        );

        when(orderService.getOrdersPage(null, 0, 20))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].orderId").value("order-abc"))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.content[0].customerId").value("customer-123"))
                .andExpect(jsonPath("$.content[0].productId").value("product-456"))
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.content[0].updatedAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.content[1].orderId").value("order-def"))
                .andExpect(jsonPath("$.content[1].status").value("CREATED"))
                .andExpect(jsonPath("$.content[1].customerId").value("customer-456"))
                .andExpect(jsonPath("$.content[1].productId").value("product-789"))
                .andExpect(jsonPath("$.content[1].quantity").value(1))
                .andExpect(jsonPath("$.content[1].createdAt").value("2026-05-17T12:05:00Z"))
                .andExpect(jsonPath("$.content[1].updatedAt").value("2026-05-17T12:05:00Z"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderService).getOrdersPage(null, 0, 20);
    }

    @Test
    void getAllOrders_whenNoOrdersExist_shouldReturnEmptyPage() throws Exception {
        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(),
                0,
                20,
                0,
                0
        );

        when(orderService.getOrdersPage(null, 0, 20))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(orderService).getOrdersPage(null, 0, 20);
    }

    @Test
    void getAllOrders_whenStatusFilterIsProvided_shouldReturnMatchingOrders() throws Exception {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        OrderResponse response = new OrderResponse(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                createdAt
        );

        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(response),
                0,
                20,
                1,
                1
        );

        when(orderService.getOrdersPage("CREATED", 0, 20))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/orders")
                        .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value("order-123"))
                .andExpect(jsonPath("$.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(orderService).getOrdersPage("CREATED", 0, 20);
    }

    @Test
    void getAllOrders_whenStatusFilterIsInvalid_shouldReturnBadRequest() throws Exception {
        when(orderService.getOrdersPage("INVALID", 0, 20))
                .thenThrow(new InvalidOrderStatusException("INVALID"));

        mockMvc.perform(get("/orders")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid order status: INVALID. Allowed values: CREATED, CANCELLED"))
                .andExpect(jsonPath("$.path").value("/orders"));

        verify(orderService).getOrdersPage("INVALID", 0, 20);
    }

    @Test
    void getAllOrders_whenPaginationParametersAreProvided_shouldPassThemToService() throws Exception {
        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(),
                1,
                5,
                12,
                3
        );

        when(orderService.getOrdersPage(null, 1, 5))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/orders")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(orderService).getOrdersPage(null, 1, 5);
    }

    @Test
    void getAllOrders_whenPageIsNegative_shouldReturnBadRequest() throws Exception {
        when(orderService.getOrdersPage(null, -1, 20))
                .thenThrow(new InvalidPaginationException("Invalid pagination parameter: page must be at least 0"));

        mockMvc.perform(get("/orders")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameter: page must be at least 0"))
                .andExpect(jsonPath("$.path").value("/orders"));

        verify(orderService).getOrdersPage(null, -1, 20);
    }

    @Test
    void cancelOrder_whenOrderExists_shouldReturnCancelledOrder() throws Exception {
        OrderResponse response = new OrderResponse(
                "order-123",
                OrderStatus.CANCELLED,
                "customer-123",
                "product-456",
                2,
                Instant.parse("2026-05-17T12:00:00Z"),
                Instant.parse("2026-05-17T12:05:00Z")
        );

        when(orderService.cancelOrder("order-123"))
                .thenReturn(response);

        mockMvc.perform(patch("/orders/{orderId}/cancel", "order-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-123"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-456"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.createdAt").value("2026-05-17T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-17T12:05:00Z"));

        verify(orderService).cancelOrder("order-123");
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_shouldReturnNotFound() throws Exception {
        when(orderService.cancelOrder("missing-order"))
                .thenThrow(new OrderNotFoundException("missing-order"));

        mockMvc.perform(patch("/orders/{orderId}/cancel", "missing-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Order not found"))
                .andExpect(jsonPath("$.message").value("Order not found: missing-order"))
                .andExpect(jsonPath("$.path").value("/orders/missing-order/cancel"));

        verify(orderService).cancelOrder("missing-order");
    }

    @Test
    void cancelOrder_whenOrderCannotBeCancelled_shouldReturnConflict() throws Exception {
        when(orderService.cancelOrder("order-123"))
                .thenThrow(new InvalidOrderStateException("order-123", OrderStatus.CANCELLED, "cancel"));

        mockMvc.perform(patch("/orders/{orderId}/cancel", "order-123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot cancel order order-123 when status is CANCELLED"))
                .andExpect(jsonPath("$.path").value("/orders/order-123/cancel"));

        verify(orderService).cancelOrder("order-123");
    }
}