package com.portal.universe.shoppingservice.product.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingservice.product.dto.ProductWithReviewsResponse;
import com.portal.universe.shoppingservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 상품(Product) 관련 CRUD API를 제공하는 컨트롤러입니다.
 *
 * 권한 정책:
 * - 조회(GET): 공개 또는 인증된 사용자 (USER, ADMIN)
 * - 생성/수정/삭제: ADMIN 전용
 *
 * 주의: Admin 전용 API는 AdminProductController에서 처리합니다.
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
    public ApiResponse<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(productService.getAllProducts(pageable));
    }

    /**
     * 새로운 상품을 등록합니다. (ADMIN 전용)
     *
     * @deprecated Admin 전용 API는 AdminProductController를 사용하세요.
     * @param request 생성할 상품의 정보를 담은 DTO
     * @return 생성된 상품 정보를 담은 ApiResponse
     */
    @Deprecated
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ApiResponse.success(productService.createProduct(request));
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
     * 특정 상품 정보를 수정합니다. (ADMIN 전용)
     *
     * @deprecated Admin 전용 API는 AdminProductController를 사용하세요.
     * @param productId 수정할 상품의 ID
     * @param request 수정할 상품의 정보를 담은 DTO
     * @return 수정된 상품 정보를 담은 ApiResponse
     */
    @Deprecated
    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long productId, @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    /**
     * 특정 상품을 삭제합니다. (ADMIN 전용)
     *
     * @deprecated Admin 전용 API는 AdminProductController를 사용하세요.
     * @param productId 삭제할 상품의 ID
     * @return 성공 응답을 담은 ApiResponse
     */
    @Deprecated
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
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
