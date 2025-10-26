package com.portal.universe.shoppingservice.service;

import com.portal.universe.shoppingservice.dto.ProductCreateRequest;
import com.portal.universe.shoppingservice.dto.ProductResponse;
import com.portal.universe.shoppingservice.dto.ProductUpdateRequest;
import com.portal.universe.shoppingservice.dto.ProductWithReviewsResponse;

/**
 * 상품 관련 비즈니스 로직을 정의하는 인터페이스입니다.
 */
public interface ProductService {
    /**
     * 새로운 상품을 생성합니다.
     * @param request 생성할 상품 정보 DTO
     * @return 생성된 상품 정보 DTO
     */
    ProductResponse createProduct(ProductCreateRequest request);

    /**
     * ID로 특정 상품을 조회합니다.
     * @param id 조회할 상품의 ID
     * @return 조회된 상품 정보 DTO
     */
    ProductResponse getProductById(Long id);

    /**
     * 기존 상품 정보를 수정합니다.
     * @param productId 수정할 상품의 ID
     * @param request 수정할 상품 정보 DTO
     * @return 수정된 상품 정보 DTO
     */
    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

    /**
     * 상품을 삭제합니다.
     * @param productId 삭제할 상품의 ID
     */
    void deleteProduct(Long productId);

    /**
     * 특정 상품 정보와 해당 상품의 리뷰 목록을 함께 조회합니다.
     * @param productId 조회할 상품의 ID
     * @return 상품 정보와 리뷰 목록을 포함한 DTO
     */
    ProductWithReviewsResponse getProductWithReviews(Long productId);
}