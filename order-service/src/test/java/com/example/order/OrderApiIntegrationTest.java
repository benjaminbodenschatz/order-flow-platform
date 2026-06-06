package com.example.order;

import com.example.order.domain.OrderStatus;
import com.example.order.model.CreateOrderRequest;
import com.example.order.model.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_api_integration_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "spring.h2.console.enabled=false"
})
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orders");
    }

    @Test
    void orderLifecycle_whenUsingApi_shouldPersistAndUpdateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer-123",
                "product-456",
                2
        );

        String createResponseJson = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-456"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderResponse createdOrder = objectMapper.readValue(createResponseJson, OrderResponse.class);
        String orderId = createdOrder.orderId();

        assertThat(createdOrder.updatedAt()).isEqualTo(createdOrder.createdAt());

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.productId").value("product-456"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].updatedAt", notNullValue()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        String cancelResponseJson = mockMvc.perform(patch("/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderResponse cancelledOrder = objectMapper.readValue(cancelResponseJson, OrderResponse.class);

        assertThat(cancelledOrder.createdAt()).isNotNull();
        assertThat(cancelledOrder.updatedAt()).isNotNull();
        assertThat(cancelledOrder.updatedAt()).isAfterOrEqualTo(cancelledOrder.createdAt());

        mockMvc.perform(get("/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        Integer cancelledRowCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM orders
                WHERE order_id = ?
                  AND status = ?
                  AND created_at IS NOT NULL
                  AND updated_at IS NOT NULL
                """,
                Integer.class,
                orderId,
                OrderStatus.CANCELLED.name()
        );

        assertThat(cancelledRowCount).isEqualTo(1);
    }

    @Test
    void createOrder_whenRequestIsInvalid_shouldReturnBadRequestAndNotPersistOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "",
                "product-456",
                0
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Integer.class
        );

        assertThat(rowCount).isZero();
    }

    @Test
    void getAllOrders_whenStatusFilterIsProvided_shouldReturnOnlyMatchingOrders() throws Exception {
        CreateOrderRequest firstRequest = new CreateOrderRequest(
                "customer-created",
                "product-created",
                2
        );

        CreateOrderRequest secondRequest = new CreateOrderRequest(
                "customer-cancelled",
                "product-cancelled",
                1
        );

        String firstCreateResponseJson = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondCreateResponseJson = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        OrderResponse createdOrder = objectMapper.readValue(firstCreateResponseJson, OrderResponse.class);
        OrderResponse orderToCancel = objectMapper.readValue(secondCreateResponseJson, OrderResponse.class);

        mockMvc.perform(patch("/orders/{orderId}/cancel", orderToCancel.orderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.CANCELLED.name()));

        mockMvc.perform(get("/orders")
                        .param("status", OrderStatus.CREATED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId").value(createdOrder.orderId()))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.CREATED.name()))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].updatedAt", notNullValue()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/orders")
                        .param("status", OrderStatus.CANCELLED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId").value(orderToCancel.orderId()))
                .andExpect(jsonPath("$.content[0].status").value(OrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].updatedAt", notNullValue()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAllOrders_whenPaginationParametersAreProvided_shouldReturnRequestedPage() throws Exception {
        CreateOrderRequest firstRequest = new CreateOrderRequest(
                "customer-111",
                "product-111",
                1
        );

        CreateOrderRequest secondRequest = new CreateOrderRequest(
                "customer-222",
                "product-222",
                2
        );

        CreateOrderRequest thirdRequest = new CreateOrderRequest(
                "customer-333",
                "product-333",
                3
        );

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(thirdRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getAllOrders_whenPageIsNegative_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/orders")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameter: page must be at least 0"))
                .andExpect(jsonPath("$.path").value("/orders"));
    }

    @Test
    void getAllOrders_whenSizeIsZero_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/orders")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameter: size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/orders"));
    }

    @Test
    void getAllOrders_whenSizeIsTooLarge_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/orders")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("Invalid pagination parameter: size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/orders"));
    }

    @Test
    void getAllOrders_whenStatusFilterIsInvalid_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/orders")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value("Invalid order status: INVALID. Allowed values: CREATED, CANCELLED"))
                .andExpect(jsonPath("$.path").value("/orders"));
    }
}