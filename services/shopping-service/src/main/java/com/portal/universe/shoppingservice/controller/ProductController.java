package com.portal.universe.shoppingservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.dto.ProductCreateRequest;
import com.portal.universe.shoppingservice.dto.ProductResponse;
import com.portal.universe.shoppingservice.dto.ProductUpdateRequest;
import com.portal.universe.shoppingservice.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 상품(Product) 관련 CRUD API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/shopping/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 새로운 상품을 등록합니다. (ADMIN 전용)
     * @param request 생성할 상품의 정보를 담은 DTO
     * @return 생성된 상품 정보를 담은 ApiResponse
     */
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    /**
     * 특정 ID를 가진 상품을 조회합니다.
     * @param productId 조회할 상품의 ID
     * @return 조회된 상품 정보를 담은 ApiResponse
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductById(productId));
    }

    /**
     * 특정 상품 정보를 수정합니다. (ADMIN 전용)
     * @param productId 수정할 상품의 ID
     * @param request 수정할 상품의 정보를 담은 DTO
     * @return 수정된 상품 정보를 담은 ApiResponse
     */
    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long productId, @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    /**
     * 특정 상품을 삭제합니다. (ADMIN 전용)
     * @param productId 삭제할 상품의 ID
     * @return 성공 응답을 담은 ApiResponse
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

    /**
     * 특정 상품 정보와 해당 상품에 대한 리뷰(블로그 게시물) 목록을 함께 조회합니다.
     * Blog 서비스와의 Feign 통신을 통해 데이터를 조합합니다.
     * @param productId 조회할 상품의 ID
     * @return 상품 정보와 리뷰 목록을 포함한 ApiResponse
     */
    @GetMapping("/{productId}/with-reviews")
    public ApiResponse<ProductWithReviewsResponse> getProductWithReviews(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductWithReviews(productId));
    }
}