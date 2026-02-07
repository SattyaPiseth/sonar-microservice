package com.pesexpo.productservice.service.impl;

import com.pesexpo.productservice.client.OrderClient;
import com.pesexpo.productservice.client.dto.OrderResponse;
import com.pesexpo.productservice.client.dto.ProductOrdersResponse;
import com.pesexpo.productservice.domain.Product;
import com.pesexpo.productservice.domain.dto.CreateProduct;
import com.pesexpo.productservice.domain.dto.ResponseProduct;
import com.pesexpo.productservice.domain.dto.UpdateProduct;
import com.pesexpo.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private final String TEST_UUID = "test-uuid-123";

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1);
        testProduct.setUuid(TEST_UUID);
        testProduct.setProductName("Test Product");
        testProduct.setPrice(new BigDecimal("99.99"));
    }

    @Nested
    @DisplayName("createProduct tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() {
            CreateProduct createProduct = new CreateProduct("New Product", new BigDecimal("49.99"));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.createProduct(createProduct);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());

            Product savedProduct = productCaptor.getValue();
            assertThat(savedProduct.getProductName()).isEqualTo("New Product");
            assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
            assertThat(savedProduct.getUuid()).isNotNull();
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return product when found")
        void shouldReturnProductWhenFound() {
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));

            ResponseProduct result = productService.findById(TEST_UUID);

            assertThat(result.uuid()).isEqualTo(TEST_UUID);
            assertThat(result.productName()).isEqualTo("Test Product");
            assertThat(result.price()).isEqualByComparingTo(new BigDecimal("99.99"));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(TEST_UUID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all products")
        void shouldReturnAllProducts() {
            Product product2 = new Product();
            product2.setId(2);
            product2.setUuid("uuid-2");
            product2.setProductName("Product 2");
            product2.setPrice(new BigDecimal("29.99"));

            when(productRepository.findAll()).thenReturn(List.of(testProduct, product2));

            List<ResponseProduct> result = productService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).productName()).isEqualTo("Test Product");
            assertThat(result.get(1).productName()).isEqualTo("Product 2");
        }

        @Test
        @DisplayName("Should return empty list when no products")
        void shouldReturnEmptyListWhenNoProducts() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            List<ResponseProduct> result = productService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateProduct tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product name and price")
        void shouldUpdateProductNameAndPrice() {
            UpdateProduct updateProduct = new UpdateProduct("Updated Name", new BigDecimal("149.99"));
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.updateProduct(TEST_UUID, updateProduct);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());

            Product savedProduct = productCaptor.getValue();
            assertThat(savedProduct.getProductName()).isEqualTo("Updated Name");
            assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
        }

        @Test
        @DisplayName("Should update only product name when price is null")
        void shouldUpdateOnlyProductName() {
            UpdateProduct updateProduct = new UpdateProduct("Updated Name", null);
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.updateProduct(TEST_UUID, updateProduct);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());

            Product savedProduct = productCaptor.getValue();
            assertThat(savedProduct.getProductName()).isEqualTo("Updated Name");
            assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        }

        @Test
        @DisplayName("Should update only price when name is null")
        void shouldUpdateOnlyPrice() {
            UpdateProduct updateProduct = new UpdateProduct(null, new BigDecimal("199.99"));
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            productService.updateProduct(TEST_UUID, updateProduct);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());

            Product savedProduct = productCaptor.getValue();
            assertThat(savedProduct.getProductName()).isEqualTo("Test Product");
            assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        }

        @Test
        @DisplayName("Should throw exception when product not found for update")
        void shouldThrowExceptionWhenProductNotFoundForUpdate() {
            UpdateProduct updateProduct = new UpdateProduct("Updated Name", new BigDecimal("149.99"));
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(TEST_UUID, updateProduct))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    @Nested
    @DisplayName("deleteProduct tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProductSuccessfully() {
            when(productRepository.existsByUuid(TEST_UUID)).thenReturn(true);
            doNothing().when(productRepository).deleteByUuid(TEST_UUID);

            productService.deleteProduct(TEST_UUID);

            verify(productRepository).existsByUuid(TEST_UUID);
            verify(productRepository).deleteByUuid(TEST_UUID);
        }

        @Test
        @DisplayName("Should throw exception when product not found for delete")
        void shouldThrowExceptionWhenProductNotFoundForDelete() {
            when(productRepository.existsByUuid(TEST_UUID)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(TEST_UUID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Product not found");

            verify(productRepository, never()).deleteByUuid(any());
        }
    }

    @Nested
    @DisplayName("findProductWithOrders tests")
    class FindProductWithOrdersTests {

        @Test
        @DisplayName("Should return product with orders")
        void shouldReturnProductWithOrders() {
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));

            OrderResponse order1 = new OrderResponse(
                    "order-uuid-1",
                    TEST_UUID,
                    2,
                    new BigDecimal("199.98"),
                    LocalDateTime.now(),
                    "PENDING"
            );
            OrderResponse order2 = new OrderResponse(
                    "order-uuid-2",
                    TEST_UUID,
                    1,
                    new BigDecimal("99.99"),
                    LocalDateTime.now(),
                    "CONFIRMED"
            );
            when(orderClient.findOrdersByProductUuid(TEST_UUID)).thenReturn(List.of(order1, order2));

            ProductOrdersResponse result = productService.findProductWithOrders(TEST_UUID);

            assertThat(result.product().uuid()).isEqualTo(TEST_UUID);
            assertThat(result.orders()).hasSize(2);
            assertThat(result.orders().get(0).uuid()).isEqualTo("order-uuid-1");
            assertThat(result.orders().get(1).uuid()).isEqualTo("order-uuid-2");
        }

        @Test
        @DisplayName("Should return product with empty orders when no orders exist")
        void shouldReturnProductWithEmptyOrders() {
            when(productRepository.findByUuid(TEST_UUID)).thenReturn(Optional.of(testProduct));
            when(orderClient.findOrdersByProductUuid(TEST_UUID)).thenReturn(Collections.emptyList());

            ProductOrdersResponse result = productService.findProductWithOrders(TEST_UUID);

            assertThat(result.product().uuid()).isEqualTo(TEST_UUID);
            assertThat(result.orders()).isEmpty();
        }
    }
}
