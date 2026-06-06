package com.example.order.service;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.error.InvalidOrderStateException;
import com.example.order.error.InvalidOrderStatusException;
import com.example.order.error.InvalidPaginationException;
import com.example.order.error.OrderNotFoundException;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import com.example.order.model.PageResponse;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        assertThat(response.updatedAt()).isNotNull();
        assertThat(response.updatedAt()).isEqualTo(response.createdAt());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.orderId()).isEqualTo(response.orderId());
        assertThat(savedOrder.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.customerId()).isEqualTo("customer-123");
        assertThat(savedOrder.productId()).isEqualTo("product-456");
        assertThat(savedOrder.quantity()).isEqualTo(2);
        assertThat(savedOrder.createdAt()).isEqualTo(response.createdAt());
        assertThat(savedOrder.updatedAt()).isEqualTo(response.updatedAt());
    }

    @Test
    void getOrderById_whenOrderExists_shouldReturnOrder() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-17T12:05:00Z");

        Order order = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                updatedAt
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
        assertThat(response.updatedAt()).isEqualTo(updatedAt);

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
        Instant firstUpdatedAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-05-17T12:01:00Z");
        Instant secondUpdatedAt = Instant.parse("2026-05-17T12:01:00Z");

        Order firstOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                firstCreatedAt,
                firstUpdatedAt
        );

        Order secondOrder = new Order(
                "order-456",
                OrderStatus.CREATED,
                "customer-456",
                "product-789",
                1,
                secondCreatedAt,
                secondUpdatedAt
        );

        when(orderRepository.findAll())
                .thenReturn(List.of(firstOrder, secondOrder));

        List<OrderResponse> responses = orderService.getAllOrders(null);

        assertThat(responses).hasSize(2);

        assertThat(responses.get(0).orderId()).isEqualTo("order-123");
        assertThat(responses.get(0).status()).isEqualTo(OrderStatus.CREATED);
        assertThat(responses.get(0).customerId()).isEqualTo("customer-123");
        assertThat(responses.get(0).productId()).isEqualTo("product-456");
        assertThat(responses.get(0).quantity()).isEqualTo(2);
        assertThat(responses.get(0).createdAt()).isEqualTo(firstCreatedAt);
        assertThat(responses.get(0).updatedAt()).isEqualTo(firstUpdatedAt);

        assertThat(responses.get(1).orderId()).isEqualTo("order-456");
        assertThat(responses.get(1).status()).isEqualTo(OrderStatus.CREATED);
        assertThat(responses.get(1).customerId()).isEqualTo("customer-456");
        assertThat(responses.get(1).productId()).isEqualTo("product-789");
        assertThat(responses.get(1).quantity()).isEqualTo(1);
        assertThat(responses.get(1).createdAt()).isEqualTo(secondCreatedAt);
        assertThat(responses.get(1).updatedAt()).isEqualTo(secondUpdatedAt);

        verify(orderRepository).findAll();
    }

    @Test
    void getAllOrders_whenNoOrdersExist_shouldReturnEmptyList() {
        when(orderRepository.findAll())
                .thenReturn(List.of());

        List<OrderResponse> responses = orderService.getAllOrders(null);

        assertThat(responses).isEmpty();

        verify(orderRepository).findAll();
    }

    @Test
    void cancelOrder_whenOrderIsCreated_shouldUpdateStatusToCancelled() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant originalUpdatedAt = Instant.parse("2026-05-17T12:00:00Z");

        Order existingOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                originalUpdatedAt
        );

        when(orderRepository.findById("order-123"))
                .thenReturn(Optional.of(existingOrder));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant beforeCancel = Instant.now();

        OrderResponse response = orderService.cancelOrder("order-123");

        Instant afterCancel = Instant.now();

        assertThat(response.orderId()).isEqualTo("order-123");
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.customerId()).isEqualTo("customer-123");
        assertThat(response.productId()).isEqualTo("product-456");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isAfterOrEqualTo(beforeCancel);
        assertThat(response.updatedAt()).isBeforeOrEqualTo(afterCancel);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).findById("order-123");
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.orderId()).isEqualTo("order-123");
        assertThat(savedOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(savedOrder.customerId()).isEqualTo("customer-123");
        assertThat(savedOrder.productId()).isEqualTo("product-456");
        assertThat(savedOrder.quantity()).isEqualTo(2);
        assertThat(savedOrder.createdAt()).isEqualTo(createdAt);
        assertThat(savedOrder.updatedAt()).isEqualTo(response.updatedAt());
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
        Instant updatedAt = Instant.parse("2026-05-17T12:05:00Z");

        Order existingOrder = new Order(
                "order-123",
                OrderStatus.CANCELLED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                updatedAt
        );

        when(orderRepository.findById("order-123"))
                .thenReturn(Optional.of(existingOrder));

        assertThatThrownBy(() -> orderService.cancelOrder("order-123"))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Cannot cancel order order-123 when status is CANCELLED");

        verify(orderRepository).findById("order-123");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getAllOrders_whenStatusFilterIsProvided_shouldReturnMatchingOrders() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant cancelledAt = Instant.parse("2026-05-17T12:05:00Z");

        Order createdOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                createdAt
        );

        Order cancelledOrder = new Order(
                "order-456",
                OrderStatus.CANCELLED,
                "customer-789",
                "product-999",
                1,
                createdAt,
                cancelledAt
        );

        when(orderRepository.findAllByStatus(OrderStatus.CREATED))
                .thenReturn(List.of(createdOrder));

        List<OrderResponse> responses = orderService.getAllOrders("CREATED");

        assertEquals(1, responses.size());
        assertEquals("order-123", responses.get(0).orderId());
        assertEquals(OrderStatus.CREATED, responses.get(0).status());

        verify(orderRepository).findAllByStatus(OrderStatus.CREATED);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void getAllOrders_whenStatusFilterUsesLowercase_shouldReturnMatchingOrders() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        Order createdOrder = new Order(
                "order-123",
                OrderStatus.CREATED,
                "customer-123",
                "product-456",
                2,
                createdAt,
                createdAt
        );

        when(orderRepository.findAllByStatus(OrderStatus.CREATED))
                .thenReturn(List.of(createdOrder));

        List<OrderResponse> responses = orderService.getAllOrders("created");

        assertEquals(1, responses.size());
        assertEquals("order-123", responses.get(0).orderId());
        assertEquals(OrderStatus.CREATED, responses.get(0).status());

        verify(orderRepository).findAllByStatus(OrderStatus.CREATED);
        verify(orderRepository, never()).findAll();
    }

    @Test
    void getAllOrders_whenStatusFilterIsInvalid_shouldThrowInvalidOrderStatusException() {
        assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.getAllOrders("INVALID")
        );

        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrdersPage_whenOrdersExist_shouldReturnPagedOrders() {
        Instant firstCreatedAt = Instant.parse("2026-05-17T12:00:00Z");
        Instant secondCreatedAt = Instant.parse("2026-05-17T12:05:00Z");

        Order firstOrder = new Order(
                "order-1",
                OrderStatus.CREATED,
                "customer-111",
                "product-111",
                1,
                firstCreatedAt,
                firstCreatedAt
        );

        Order secondOrder = new Order(
                "order-2",
                OrderStatus.CREATED,
                "customer-222",
                "product-222",
                2,
                secondCreatedAt,
                secondCreatedAt
        );

        when(orderRepository.findAll(2, 0)).thenReturn(List.of(firstOrder, secondOrder));
        when(orderRepository.countAll()).thenReturn(5L);

        PageResponse<OrderResponse> response = orderService.getOrdersPage(null, 0, 2);

        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(2, response.content().size());
        assertEquals("order-1", response.content().get(0).orderId());
        assertEquals("order-2", response.content().get(1).orderId());
    }

    @Test
    void getOrdersPage_whenPageIsOne_shouldUseCorrectOffset() {
        when(orderRepository.findAll(2, 2)).thenReturn(List.of());
        when(orderRepository.countAll()).thenReturn(5L);

        PageResponse<OrderResponse> response = orderService.getOrdersPage(null, 1, 2);

        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());

        verify(orderRepository).findAll(2, 2);
    }

    @Test
    void getOrdersPage_whenStatusFilterIsProvided_shouldReturnPagedMatchingOrders() {
        Instant createdAt = Instant.parse("2026-05-17T12:00:00Z");

        Order order = new Order(
                "order-1",
                OrderStatus.CREATED,
                "customer-111",
                "product-111",
                1,
                createdAt,
                createdAt
        );

        when(orderRepository.findAllByStatus(OrderStatus.CREATED, 10, 0)).thenReturn(List.of(order));
        when(orderRepository.countByStatus(OrderStatus.CREATED)).thenReturn(1L);

        PageResponse<OrderResponse> response = orderService.getOrdersPage("CREATED", 0, 10);

        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(1, response.content().size());
        assertEquals(OrderStatus.CREATED, response.content().get(0).status());
    }

    @Test
    void getOrdersPage_whenPageIsNegative_shouldThrowInvalidPaginationException() {
        assertThrows(
                InvalidPaginationException.class,
                () -> orderService.getOrdersPage(null, -1, 20)
        );
    }

    @Test
    void getOrdersPage_whenSizeIsZero_shouldThrowInvalidPaginationException() {
        assertThrows(
                InvalidPaginationException.class,
                () -> orderService.getOrdersPage(null, 0, 0)
        );
    }

    @Test
    void getOrdersPage_whenSizeIsTooLarge_shouldThrowInvalidPaginationException() {
        assertThrows(
                InvalidPaginationException.class,
                () -> orderService.getOrdersPage(null, 0, 101)
        );
    }
}