package com.pesexpo.productservice.controller;

import com.pesexpo.productservice.client.dto.OrderResponse;
import com.pesexpo.productservice.client.dto.ProductOrdersResponse;
import com.pesexpo.productservice.domain.dto.ResponseProduct;
import com.pesexpo.productservice.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void shouldCreateProduct() throws Exception {
        doNothing().when(productService).createProduct(org.mockito.ArgumentMatchers.any());

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Widget\",\"price\":9.99}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Product created successfully"));
    }

    @Test
    void shouldGetAllProducts() throws Exception {
        ResponseProduct response = ResponseProduct.builder()
                .uuid("prod-1")
                .productName("Widget")
                .price(new BigDecimal("9.99"))
                .build();
        when(productService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value("prod-1"));
    }

    @Test
    void shouldGetProductById() throws Exception {
        ResponseProduct response = ResponseProduct.builder()
                .uuid("prod-1")
                .productName("Widget")
                .price(new BigDecimal("9.99"))
                .build();
        when(productService.findById("prod-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{uuid}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("prod-1"));
    }

    @Test
    void shouldReturnNotFoundForMissingProduct() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"))
                .when(productService).findById("missing");

        mockMvc.perform(get("/api/v1/products/{uuid}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        doNothing().when(productService).updateProduct(org.mockito.ArgumentMatchers.eq("prod-1"),
                org.mockito.ArgumentMatchers.any());

        mockMvc.perform(put("/api/v1/products/{uuid}", "prod-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Updated\",\"price\":12.00}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product updated successfully"));

        verify(productService).updateProduct(org.mockito.ArgumentMatchers.eq("prod-1"),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productService).deleteProduct("prod-1");

        mockMvc.perform(delete("/api/v1/products/{uuid}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Product deleted successfully"));

        verify(productService).deleteProduct("prod-1");
    }

    @Test
    void shouldGetProductWithOrders() throws Exception {
        ResponseProduct product = ResponseProduct.builder()
                .uuid("prod-1")
                .productName("Widget")
                .price(new BigDecimal("9.99"))
                .build();
        OrderResponse order = new OrderResponse(
                "order-1",
                "prod-1",
                1,
                new BigDecimal("9.99"),
                LocalDateTime.of(2024, 1, 1, 10, 0),
                "PENDING"
        );
        ProductOrdersResponse response = ProductOrdersResponse.builder()
                .product(product)
                .orders(List.of(order))
                .build();
        when(productService.findProductWithOrders("prod-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{uuid}/orders", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.uuid").value("prod-1"))
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].uuid").value("order-1"));
    }
}
