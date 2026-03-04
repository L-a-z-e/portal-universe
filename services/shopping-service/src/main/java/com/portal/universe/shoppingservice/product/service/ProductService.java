package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 상품 조회 비즈니스 로직을 정의하는 인터페이스입니다.
 * CUD 작업은 seller-service에서 수행되고 Kafka 이벤트로 동기화됩니다.
 */
public interface ProductService {
    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> getProductsByCategory(String category, Pageable pageable);

    List<String> getAllCategories();

    ProductResponse getProductById(Long id);

    ProductWithReviewsResponse getProductWithReviews(Long productId);
}
