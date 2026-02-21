package com.portal.universe.shoppingservice.product.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품(Product) 조회 API를 제공하는 컨트롤러입니다. (Buyer 전용)
 *
 * 상품 CRUD(생성/수정/삭제)는 shopping-seller-service로 이전되었습니다.
 * 이 컨트롤러는 읽기 전용 조회 API만 제공합니다.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 페이징된 상품 목록을 조회합니다. (공개)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 12)
     * @return 페이징된 상품 목록을 담은 ApiResponse
     */
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category) {
        Pageable pageable = PageRequest.of(page - 1, size);
        if (category != null && !category.isBlank()) {
            return ApiResponse.success(PageResponse.from(productService.getProductsByCategory(category, pageable)));
        }
        return ApiResponse.success(PageResponse.from(productService.getAllProducts(pageable)));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(productService.getAllCategories());
    }

    /**
     * 특정 ID를 가진 상품을 조회합니다. (공개)
     * @param productId 조회할 상품의 ID
     * @return 조회된 상품 정보를 담은 ApiResponse
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductById(productId));
    }

    /**
     * 특정 상품 정보와 해당 상품에 대한 리뷰(블로그 게시물) 목록을 함께 조회합니다. (공개)
     * Blog 서비스와의 Feign 통신을 통해 데이터를 조합합니다.
     * @param productId 조회할 상품의 ID
     * @return 상품 정보와 리뷰 목록을 포함한 ApiResponse
     */
    @GetMapping("/{productId}/with-reviews")
    public ApiResponse<ProductWithReviewsResponse> getProductWithReviews(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductWithReviews(productId));
    }
}
