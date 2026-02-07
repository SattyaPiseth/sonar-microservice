package com.pesexpo.orderservice.service.impl;

import com.pesexpo.orderservice.client.ProductClient;
import com.pesexpo.orderservice.client.dto.ProductResponse;
import com.pesexpo.orderservice.domain.Order;
import com.pesexpo.orderservice.domain.OrderStatus;
import com.pesexpo.orderservice.domain.dto.CreateOrder;
import com.pesexpo.orderservice.domain.dto.ResponseOrder;
import com.pesexpo.orderservice.repository.OrderRepository;
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
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private ProductResponse testProduct;
    private final String TEST_ORDER_UUID = "order-uuid-123";
    private final String TEST_PRODUCT_UUID = "product-uuid-456";

    @BeforeEach
    void setUp() {
        testProduct = new ProductResponse(
                TEST_PRODUCT_UUID,
                "Test Product",
                new BigDecimal("99.99")
        );

        testOrder = new Order();
        testOrder.setId(1);
        testOrder.setUuid(TEST_ORDER_UUID);
        testOrder.setProductUuid(TEST_PRODUCT_UUID);
        testOrder.setQuantity(2);
        testOrder.setTotalPrice(new BigDecimal("199.98"));
        testOrder.setOrderDate(LocalDateTime.of(2024, 1, 15, 10, 30));
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Nested
    @DisplayName("createOrder tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() {
            CreateOrder createOrder = new CreateOrder(TEST_PRODUCT_UUID, 3);
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID)).thenReturn(testProduct);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            orderService.createOrder(createOrder);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getProductUuid()).isEqualTo(TEST_PRODUCT_UUID);
            assertThat(savedOrder.getQuantity()).isEqualTo(3);
            assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("299.97"));
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(savedOrder.getUuid()).isNotNull();
            assertThat(savedOrder.getOrderDate()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate total price correctly")
        void shouldCalculateTotalPriceCorrectly() {
            CreateOrder createOrder = new CreateOrder(TEST_PRODUCT_UUID, 5);
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID)).thenReturn(testProduct);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            orderService.createOrder(createOrder);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            // 99.99 * 5 = 499.95
            assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("499.95"));
        }
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all orders")
        void shouldReturnAllOrders() {
            Order order2 = new Order();
            order2.setId(2);
            order2.setUuid("order-uuid-2");
            order2.setProductUuid(TEST_PRODUCT_UUID);
            order2.setQuantity(1);
            order2.setTotalPrice(new BigDecimal("99.99"));
            order2.setOrderDate(LocalDateTime.of(2024, 1, 16, 14, 0));
            order2.setStatus(OrderStatus.CONFIRMED);

            when(orderRepository.findAll()).thenReturn(List.of(testOrder, order2));
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID)).thenReturn(testProduct);

            List<ResponseOrder> result = orderService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).uuid()).isEqualTo(TEST_ORDER_UUID);
            assertThat(result.get(0).quantity()).isEqualTo(2);
            assertThat(result.get(1).uuid()).isEqualTo("order-uuid-2");
            assertThat(result.get(1).quantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty list when no orders")
        void shouldReturnEmptyListWhenNoOrders() {
            when(orderRepository.findAll()).thenReturn(Collections.emptyList());

            List<ResponseOrder> result = orderService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUuid tests")
    class FindByUuidTests {

        @Test
        @DisplayName("Should return order when found")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findByUuid(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID)).thenReturn(testProduct);

            ResponseOrder result = orderService.findByUuid(TEST_ORDER_UUID);

            assertThat(result.uuid()).isEqualTo(TEST_ORDER_UUID);
            assertThat(result.product().uuid()).isEqualTo(TEST_PRODUCT_UUID);
            assertThat(result.product().productName()).isEqualTo("Test Product");
            assertThat(result.quantity()).isEqualTo(2);
            assertThat(result.totalPrice()).isEqualByComparingTo(new BigDecimal("199.98"));
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByUuid(TEST_ORDER_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findByUuid(TEST_ORDER_UUID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should propagate error when product service fails")
        void shouldPropagateWhenProductServiceFails() {
            when(orderRepository.findByUuid(TEST_ORDER_UUID)).thenReturn(Optional.of(testOrder));
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID))
                    .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Downstream error"));

            assertThatThrownBy(() -> orderService.findByUuid(TEST_ORDER_UUID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Downstream error");
        }
    }

    @Nested
    @DisplayName("findByProductUuid tests")
    class FindByProductUuidTests {

        @Test
        @DisplayName("Should return orders for product")
        void shouldReturnOrdersForProduct() {
            Order order2 = new Order();
            order2.setId(2);
            order2.setUuid("order-uuid-2");
            order2.setProductUuid(TEST_PRODUCT_UUID);
            order2.setQuantity(3);
            order2.setTotalPrice(new BigDecimal("299.97"));
            order2.setOrderDate(LocalDateTime.of(2024, 1, 17, 9, 0));
            order2.setStatus(OrderStatus.SHIPPED);

            when(orderRepository.findByProductUuid(TEST_PRODUCT_UUID)).thenReturn(List.of(testOrder, order2));
            when(productClient.findProductByUuid(TEST_PRODUCT_UUID)).thenReturn(testProduct);

            List<ResponseOrder> result = orderService.findByProductUuid(TEST_PRODUCT_UUID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).uuid()).isEqualTo(TEST_ORDER_UUID);
            assertThat(result.get(1).uuid()).isEqualTo("order-uuid-2");
        }

        @Test
        @DisplayName("Should return empty list when no orders for product")
        void shouldReturnEmptyListWhenNoOrdersForProduct() {
            when(orderRepository.findByProductUuid(TEST_PRODUCT_UUID)).thenReturn(Collections.emptyList());

            List<ResponseOrder> result = orderService.findByProductUuid(TEST_PRODUCT_UUID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteOrder tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("Should delete order successfully")
        void shouldDeleteOrderSuccessfully() {
            when(orderRepository.existsByUuid(TEST_ORDER_UUID)).thenReturn(true);
            doNothing().when(orderRepository).deleteByUuid(TEST_ORDER_UUID);

            orderService.deleteOrder(TEST_ORDER_UUID);

            verify(orderRepository).existsByUuid(TEST_ORDER_UUID);
            verify(orderRepository).deleteByUuid(TEST_ORDER_UUID);
        }

        @Test
        @DisplayName("Should throw exception when order not found for delete")
        void shouldThrowExceptionWhenOrderNotFoundForDelete() {
            when(orderRepository.existsByUuid(TEST_ORDER_UUID)).thenReturn(false);

            assertThatThrownBy(() -> orderService.deleteOrder(TEST_ORDER_UUID))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Order not found");

            verify(orderRepository, never()).deleteByUuid(any());
        }
    }
}
