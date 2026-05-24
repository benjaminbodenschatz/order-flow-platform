package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.error.InvalidOrderStateException;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository);
    }

    @Test
    void createOrder_shouldCreateAndSaveOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-123",
                "product-456",
                2
        );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.orderId()).isNotBlank();
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.productId()).isEqualTo("product-456");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.createdAt()).isNotNull();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.orderId()).isEqualTo(response.orderId());
        assertThat(savedOrder.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.customerId()).isEqualTo("customer-123");
        assertThat(savedOrder.productId()).isEqualTo("product-456");
        assertThat(savedOrder.quantity()).isEqualTo(2);
        assertThat(savedOrder.createdAt()).isEqualTo(response.createdAt());
    }

    @Test
    void getOrderById_whenOrderExists_shouldReturnOrder() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        Order order = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt
        );

        when(orderRepository.findById("order-123"))
                .thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("order-123");

        assertThat(response.orderId()).isEqualTo("order-123");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.productId()).isEqualTo("product-456");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verify(orderRepository).findById("order-123");
    }

    @Test
    void getOrderById_whenOrderDoesNotExist_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById("missing-order"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("missing-order"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found: missing-order");

        verify(orderRepository).findById("missing-order");
    }

    @Test
    void getAllOrders_whenOrdersExist_shouldReturnAllOrders() {
        Instant firstCreatedAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-05-17T12:01:00Z");

        Order firstOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                firstCreatedAt
        );

        Order secondOrder = new Order(
                "order-456",
                OrderStatus.CREATED,
                "customer-456",
                "product-789",
                1,
                secondCreatedAt
        );

        when(orderRepository.findAll())
                .thenReturn(List.of(firstOrder, secondOrder));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).orderId()).isEqualTo("order-123");
        assertThat(responses.get(0).status()).isEqualTo(OrderStatus.CREATED);
        assertThat(responses.get(0).customerId()).isEqualTo("customer-123");
        assertThat(responses.get(0).productId()).isEqualTo("product-456");
        assertThat(responses.get(0).quantity()).isEqualTo(2);
        assertThat(responses.get(0).createdAt()).isEqualTo(firstCreatedAt);

        assertThat(responses.get(1).orderId()).isEqualTo("order-456");
        assertThat(responses.get(1).status()).isEqualTo(OrderStatus.CREATED);
        assertThat(responses.get(1).customerId()).isEqualTo("customer-456");
        assertThat(responses.get(1).productId()).isEqualTo("product-789");
        assertThat(responses.get(1).quantity()).isEqualTo(1);
        assertThat(responses.get(1).createdAt()).isEqualTo(secondCreatedAt);

        verify(orderRepository).findAll();
    }

    @Test
    void getAllOrders_whenNoOrdersExist_shouldReturnEmptyList() {
        when(orderRepository.findAll())
                .thenReturn(List.of());

        List<OrderResponse> responses = orderService.getAllOrders();

        assertThat(responses).isEmpty();

        verify(orderRepository).findAll();
    }

    @Test
    void cancelOrder_whenOrderIsCreated_shouldUpdateStatusToCancelled() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        Order existingOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt
        );

        Order cancelledOrder = new Order(
                "order-123",
                OrderStatus.CANCELLED,
                "customer-123",
                "product-456",
                2,
                createdAt
        );

        when(orderRepository.findById("order-123"))
                .thenReturn(Optional.of(existingOrder));

        when(orderRepository.save(cancelledOrder))
                .thenReturn(cancelledOrder);

        OrderResponse response = orderService.cancelOrder("order-123");

        assertThat(response.orderId()).isEqualTo("order-123");
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.productId()).isEqualTo("product-456");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.createdAt()).isEqualTo(createdAt);

        verify(orderRepository).findById("order-123");
        verify(orderRepository).save(cancelledOrder);
    }

    @Test
    void cancelOrder_whenOrderDoesNotExist_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById("missing-order"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder("missing-order"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found: missing-order");

        verify(orderRepository).findById("missing-order");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_whenOrderIsAlreadyCancelled_shouldThrowInvalidOrderStateException() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        Order existingOrder = new Order(
                "order-123",
                OrderStatus.CANCELLED,
                "customer-123",
                "product-456",
                2,
                createdAt
        );

        when(orderRepository.findById("order-123"))
                .thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123"))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Cannot cancel order order-123 when status is CANCELLED");

        verify(orderRepository).findById("order-123");
        verify(orderRepository, never()).save(any());
    }
}