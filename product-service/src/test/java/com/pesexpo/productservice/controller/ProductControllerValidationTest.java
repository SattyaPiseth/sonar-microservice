package com.pesexpo.productservice.controller;

import com.pesexpo.productservice.exception.RestExceptionHandler;
import com.pesexpo.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(RestExceptionHandler.class)
class ProductControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void shouldRejectEmptyUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/v1/products/{uuid}", "test-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("At least one field must be provided")));
    }

    @Test
    void shouldRejectBlankProductName() throws Exception {
        mockMvc.perform(put("/api/v1/products/{uuid}", "test-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Product Name can't be blank")));
    }

    @Test
    void shouldRejectNegativePrice() throws Exception {
        mockMvc.perform(put("/api/v1/products/{uuid}", "test-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Product Price must be positive")));
    }
}
