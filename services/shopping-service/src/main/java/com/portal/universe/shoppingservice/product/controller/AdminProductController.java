package com.portal.universe.shoppingservice.product.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.product.dto.AdminProductRequest;
import com.portal.universe.shoppingservice.product.dto.ProductResponse;
import com.portal.universe.shoppingservice.product.dto.StockUpdateRequest;
import com.portal.universe.shoppingservice.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 전용 상품 관리 API를 제공하는 컨트롤러입니다.
 * 모든 엔드포인트는 ADMIN 권한이 필요합니다.
 */
@RestController
@RequestMapping("/api/shopping/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /**
     * 새로운 상품을 등록합니다. (ADMIN 전용)
     * @param request 생성할 상품의 정보를 담은 DTO (유효성 검사 포함)
     * @return 생성된 상품 정보를 담은 ApiResponse (HTTP 201 Created)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody AdminProductRequest request) {
        ProductResponse response = productService.createProductAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 특정 상품 정보를 수정합니다. (ADMIN 전용)
     * @param productId 수정할 상품의 ID
     * @param request 수정할 상품의 정보를 담은 DTO (유효성 검사 포함)
     * @return 수정된 상품 정보를 담은 ApiResponse (HTTP 200 OK)
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody AdminProductRequest request) {
        ProductResponse response = productService.updateProductAdmin(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 상품을 삭제합니다. (ADMIN 전용)
     * @param productId 삭제할 상품의 ID
     * @return 성공 응답을 담은 ApiResponse (HTTP 200 OK)
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 특정 상품의 재고를 수정합니다. (ADMIN 전용)
     * @param productId 재고를 수정할 상품의 ID
     * @param request 재고 수정 정보를 담은 DTO (유효성 검사 포함)
     * @return 수정된 상품 정보를 담은 ApiResponse (HTTP 200 OK)
     */
    @PatchMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStock(
            @PathVariable Long productId,
            @Valid @RequestBody StockUpdateRequest request) {
        ProductResponse response = productService.updateProductStock(productId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
