package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product createProduct(Long id, String name, BigDecimal price) {
        Product product = Product.builder()
                .name(name)
                .description("Test description")
                .price(price)
                .imageUrl("http://img.test/1.jpg")
                .category("Electronics")
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        @DisplayName("should_returnPagedProducts_when_called")
        void should_returnPagedProducts_when_called() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000));
            Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // when
            Page<ProductResponse> result = productService.getAllProducts(pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Product1");
            verify(productRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should_returnProduct_when_found")
        void should_returnProduct_when_found() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when
            ProductResponse result = productService.getProductById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Product1");
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should_throwException_when_notFound")
        void should_throwException_when_notFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(CustomBusinessException.class);
            verify(productRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("getProductWithReviews")
    class GetProductWithReviews {

        @Test
        @DisplayName("should_returnProductWithEmptyReviews_when_called")
        void should_returnProductWithEmptyReviews_when_called() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            // when
            ProductWithReviewsResponse result = productService.getProductWithReviews(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Product1");
            assertThat(result.reviews()).isEmpty();
            verify(productRepository).findById(1L);
        }
    }
}
