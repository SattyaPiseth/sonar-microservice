package com.pesexpo.orderservice.controller;

import com.pesexpo.orderservice.client.dto.ProductResponse;
import com.pesexpo.orderservice.domain.OrderStatus;
import com.pesexpo.orderservice.domain.dto.ResponseOrder;
import com.pesexpo.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrder() throws Exception {
        doNothing().when(orderService).createOrder(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productUuid\":\"prod-1\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order created successfully"));

        verify(orderService).createOrder(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectInvalidCreateOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnAllOrders() throws Exception {
        ResponseOrder response = ResponseOrder.builder()
                .uuid("order-1")
                .product(new ProductResponse("prod-1", "Product", new BigDecimal("10.00")))
                .quantity(1)
                .totalPrice(new BigDecimal("10.00"))
                .orderDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .status(OrderStatus.PENDING)
                .build();
        when(orderService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value("order-1"));
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"))
                .when(orderService).findByUuid("missing");

        mockMvc.perform(get("/api/v1/orders/{uuid}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteOrder() throws Exception {
        doNothing().when(orderService).deleteOrder("order-1");

        mockMvc.perform(delete("/api/v1/orders/{uuid}", "order-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Order deleted successfully"));

        verify(orderService).deleteOrder("order-1");
    }
}
