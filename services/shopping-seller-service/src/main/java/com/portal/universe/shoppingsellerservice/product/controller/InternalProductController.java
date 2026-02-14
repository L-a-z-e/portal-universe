package com.portal.universe.shoppingsellerservice.product.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getProducts(Pageable pageable) {
        return ApiResponse.success(productService.getAllProducts(pageable));
    }
}
