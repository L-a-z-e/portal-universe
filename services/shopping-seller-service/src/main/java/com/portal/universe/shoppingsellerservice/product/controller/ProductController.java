package com.portal.universe.shoppingsellerservice.product.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingsellerservice.product.service.ProductService;
import com.portal.universe.shoppingsellerservice.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SellerService sellerService;

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ApiResponse.success(productService.getAllProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ProductCreateRequest request) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(productService.createProduct(sellerId, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
            @AuthenticationPrincipal String userId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        Long sellerId = getSellerIdFromUser(userId);
        return ApiResponse.success(productService.updateProduct(sellerId, productId, request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @AuthenticationPrincipal String userId,
            @PathVariable Long productId) {
        Long sellerId = getSellerIdFromUser(userId);
        productService.deleteProduct(sellerId, productId);
        return ApiResponse.success(null);
    }

    private Long getSellerIdFromUser(String userId) {
        return sellerService.getMyInfo(userId).id();
    }
}
