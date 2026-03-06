package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.config.CacheType;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProductService 인터페이스의 구현 클래스입니다.
 * Read Model 전용 — CUD는 seller-service에서 Kafka 이벤트로 동기화됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(this::convertToResponse);
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        Page<Product> productPage = productRepository.findByCategory(category, pageable);
        return productPage.map(this::convertToResponse);
    }

    @Override
    @Cacheable(value = CacheType.Names.PRODUCT_CATEGORIES)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    @Override
    @Cacheable(value = CacheType.Names.PRODUCT_DETAIL, key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));
        return convertToResponse(product);
    }

    @Override
    public ProductWithReviewsResponse getProductWithReviews(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));
        return new ProductWithReviewsResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory(),
                List.of()
        );
    }

    private ProductResponse convertToResponse(Product product) {
        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream()
                    .map(com.portal.universe.shoppingservice.product.domain.ProductImage::getImageUrl)
                    .toList()
                : List.of();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getImageUrl(),
                product.getCategory(),
                product.getFeatured(),
                imageUrls,
                null,
                null,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

}
