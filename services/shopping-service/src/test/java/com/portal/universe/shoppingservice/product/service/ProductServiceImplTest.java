package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.feign.BlogServiceClient;
import com.portal.universe.shoppingservice.feign.dto.BlogResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.dto.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BlogServiceClient blogServiceClient;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product createProduct(Long id, String name, BigDecimal price, Integer stock) {
        Product product = Product.builder()
                .name(name)
                .description("Test description")
                .price(price)
                .stock(stock)
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
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000), 10);
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
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("should_createProduct_when_validRequest")
        void should_createProduct_when_validRequest() {
            // given
            ProductCreateRequest request = new ProductCreateRequest("New Product", "desc", BigDecimal.valueOf(5000), 20);
            Product savedProduct = createProduct(1L, "New Product", BigDecimal.valueOf(5000), 20);
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // when
            ProductResponse result = productService.createProduct(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("New Product");
            assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            verify(productRepository).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        @DisplayName("should_returnProduct_when_found")
        void should_returnProduct_when_found() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000), 10);
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
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("should_updateProduct_when_valid")
        void should_updateProduct_when_valid() {
            // given
            Product product = createProduct(1L, "Old Name", BigDecimal.valueOf(1000), 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            ProductUpdateRequest request = new ProductUpdateRequest("New Name", "new desc", BigDecimal.valueOf(2000), 20);

            // when
            ProductResponse result = productService.updateProduct(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("New Name");
            assertThat(result.price()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            verify(productRepository).findById(1L);
        }

        @Test
        @DisplayName("should_throwException_when_productNotFound")
        void should_throwException_when_productNotFound() {
            // given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());
            ProductUpdateRequest request = new ProductUpdateRequest("Name", "desc", BigDecimal.valueOf(1000), 10);

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(999L, request))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("should_deleteProduct_when_exists")
        void should_deleteProduct_when_exists() {
            // given
            when(productRepository.existsById(1L)).thenReturn(true);

            // when
            productService.deleteProduct(1L);

            // then
            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should_throwException_when_productNotFound")
        void should_throwException_when_productNotFound() {
            // given
            when(productRepository.existsById(999L)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> productService.deleteProduct(999L))
                    .isInstanceOf(CustomBusinessException.class);
        }
    }

    @Nested
    @DisplayName("getProductWithReviews")
    class GetProductWithReviews {

        @Test
        @DisplayName("should_returnProductWithReviews_when_success")
        void should_returnProductWithReviews_when_success() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000), 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            List<BlogResponse> reviews = List.of();
            when(blogServiceClient.getPostByProductId("1")).thenReturn(reviews);

            // when
            ProductWithReviewsResponse result = productService.getProductWithReviews(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Product1");
            assertThat(result.reviews()).isEmpty();
            verify(blogServiceClient).getPostByProductId("1");
        }

        @Test
        @DisplayName("should_returnEmptyReviews_when_feignFails")
        void should_returnEmptyReviews_when_feignFails() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000), 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(blogServiceClient.getPostByProductId("1")).thenThrow(new RuntimeException("Feign Error"));

            // when
            ProductWithReviewsResponse result = productService.getProductWithReviews(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.reviews()).isEmpty();
        }
    }

    @Nested
    @DisplayName("createProductAdmin")
    class CreateProductAdmin {

        @Test
        @DisplayName("should_createProduct_when_validAdminRequest")
        void should_createProduct_when_validAdminRequest() {
            // given
            AdminProductRequest request = new AdminProductRequest("Admin Product", "desc", BigDecimal.valueOf(3000), 50, "http://img.test/1.jpg", "Electronics");
            Product savedProduct = createProduct(1L, "Admin Product", BigDecimal.valueOf(3000), 50);

            when(productRepository.existsByName("Admin Product")).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // when
            ProductResponse result = productService.createProductAdmin(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Admin Product");
            verify(inventoryService).initializeInventory(1L, 50, "SYSTEM");
        }

        @Test
        @DisplayName("should_throwException_when_nameAlreadyExists")
        void should_throwException_when_nameAlreadyExists() {
            // given
            AdminProductRequest request = new AdminProductRequest("Existing Product", "desc", BigDecimal.valueOf(3000), 50, null, null);
            when(productRepository.existsByName("Existing Product")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> productService.createProductAdmin(request))
                    .isInstanceOf(CustomBusinessException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateProductAdmin")
    class UpdateProductAdmin {

        @Test
        @DisplayName("should_updateProduct_when_validAdminRequest")
        void should_updateProduct_when_validAdminRequest() {
            // given
            Product product = createProduct(1L, "Old Name", BigDecimal.valueOf(1000), 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.existsByName("New Name")).thenReturn(false);

            AdminProductRequest request = new AdminProductRequest("New Name", "new desc", BigDecimal.valueOf(2000), 30, "http://img.test/2.jpg", "Books");

            // when
            ProductResponse result = productService.updateProductAdmin(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("New Name");
        }
    }

    @Nested
    @DisplayName("updateProductStock")
    class UpdateProductStock {

        @Test
        @DisplayName("should_updateStock_when_valid")
        void should_updateStock_when_valid() {
            // given
            Product product = createProduct(1L, "Product1", BigDecimal.valueOf(1000), 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            StockUpdateRequest request = new StockUpdateRequest(50);

            // when
            ProductResponse result = productService.updateProductStock(1L, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.stock()).isEqualTo(50);
        }
    }
}
