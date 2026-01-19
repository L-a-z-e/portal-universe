package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.shoppingservice.product.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 상품 관련 비즈니스 로직을 정의하는 인터페이스입니다.
 */
public interface ProductService {
    /**
     * 페이징된 상품 목록을 조회합니다.
     * @param pageable 페이징 정보
     * @return 페이징된 상품 목록
     */
    Page<ProductResponse> getAllProducts(Pageable pageable);

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

    // ========================================
    // Admin 전용 메서드
    // ========================================

    /**
     * Admin: 새로운 상품을 생성합니다.
     * @param request Admin 상품 생성 요청 DTO
     * @return 생성된 상품 정보 DTO
     */
    ProductResponse createProductAdmin(AdminProductRequest request);

    /**
     * Admin: 기존 상품 정보를 수정합니다.
     * @param productId 수정할 상품의 ID
     * @param request Admin 상품 수정 요청 DTO
     * @return 수정된 상품 정보 DTO
     */
    ProductResponse updateProductAdmin(Long productId, AdminProductRequest request);

    /**
     * Admin: 상품 재고를 수정합니다.
     * @param productId 재고를 수정할 상품의 ID
     * @param request 재고 수정 요청 DTO
     * @return 수정된 상품 정보 DTO
     */
    ProductResponse updateProductStock(Long productId, StockUpdateRequest request);
}
