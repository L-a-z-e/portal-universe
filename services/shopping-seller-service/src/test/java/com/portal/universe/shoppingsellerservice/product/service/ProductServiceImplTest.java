package com.portal.universe.shoppingsellerservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.event.outbox.OutboxEvent;
import com.portal.universe.shoppingsellerservice.event.outbox.OutboxEventRepository;
import com.portal.universe.shoppingsellerservice.product.domain.Product;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import com.portal.universe.shoppingsellerservice.support.fixture.ProductFixture;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        productService = new ProductServiceImpl(productRepository, outboxEventRepository, meterRegistry);
        productService.initMetrics();
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should create product and save outbox event")
        void should_create_product() {
            // given
            Long sellerId = 1L;
            ProductCreateRequest request = new ProductCreateRequest(
                    "New Product", "Description", new BigDecimal("15000"),
                    new BigDecimal("12000"), 50, "https://img.test.com/new.jpg", "FASHION", true
            );
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product saved = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", 1L);
                return saved;
            });

            // when
            ProductResponse response = productService.createProduct(sellerId, request);

            // then
            assertThat(response.name()).isEqualTo("New Product");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(response.sellerId()).isEqualTo(sellerId);
            verify(productRepository).save(any(Product.class));
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("should return product when found")
        void should_return_product() {
            // given
            Product product = ProductFixture.builder().id(1L).build();
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when
            ProductResponse response = productService.getProduct(1L);

            // then
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo(ProductFixture.DEFAULT_NAME);
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_FOUND when not found")
        void should_throw_when_not_found() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProduct(999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getSellerProducts")
    class GetSellerProducts {

        @Test
        @DisplayName("should return seller's products with pagination")
        void should_return_seller_products() {
            // given
            Long sellerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Product product = ProductFixture.builder().id(1L).sellerId(sellerId).build();
            when(productRepository.findBySellerId(sellerId, pageable))
                    .thenReturn(new PageImpl<>(List.of(product)));

            // when
            Page<ProductResponse> result = productService.getSellerProducts(sellerId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).sellerId()).isEqualTo(sellerId);
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should return all products with pagination")
        void should_return_all_products() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Product p1 = ProductFixture.builder().id(1L).build();
            Product p2 = ProductFixture.builder().id(2L).name("Other").build();
            when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(p1, p2)));

            // when
            Page<ProductResponse> result = productService.getAllProducts(pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should update product when owned by seller")
        void should_update_product() {
            // given
            Long sellerId = 1L;
            Long productId = 1L;
            Product product = ProductFixture.builder().id(productId).sellerId(sellerId).build();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "Updated Name", "Updated desc", new BigDecimal("20000"),
                    new BigDecimal("18000"), 200, "https://img.test.com/updated.jpg", "CLOTHING", true
            );
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // when
            ProductResponse response = productService.updateProduct(sellerId, productId, request);

            // then
            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.price()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(response.discountPrice()).isEqualByComparingTo(new BigDecimal("18000"));
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_FOUND when product does not exist")
        void should_throw_when_not_found() {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "N", "D", BigDecimal.ONE, null, 1, null, null, null
            );
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(1L, 999L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_OWNED when seller does not own the product")
        void should_throw_when_not_owned() {
            // given
            Long otherSellerId = 99L;
            Product product = ProductFixture.builder().id(1L).sellerId(1L).build();
            ProductUpdateRequest request = new ProductUpdateRequest(
                    "N", "D", BigDecimal.ONE, null, 1, null, null, null
            );
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(otherSellerId, 1L, request))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_OWNED));
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should delete product when owned by seller")
        void should_delete_product() {
            // given
            Long sellerId = 1L;
            Long productId = 1L;
            Product product = ProductFixture.builder().id(productId).sellerId(sellerId).build();
            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // when
            productService.deleteProduct(sellerId, productId);

            // then
            verify(productRepository).delete(product);
            verify(outboxEventRepository).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_FOUND when product does not exist")
        void should_throw_when_not_found() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.deleteProduct(1L, 999L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_FOUND));
        }

        @Test
        @DisplayName("should throw PRODUCT_NOT_OWNED when seller does not own the product")
        void should_throw_when_not_owned() {
            // given
            Product product = ProductFixture.builder().id(1L).sellerId(1L).build();
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when & then
            assertThatThrownBy(() -> productService.deleteProduct(99L, 1L))
                    .isInstanceOf(CustomBusinessException.class)
                    .satisfies(ex -> assertThat(((CustomBusinessException) ex).getErrorCode())
                            .isEqualTo(SellerErrorCode.PRODUCT_NOT_OWNED));
        }
    }
}
