package com.portal.universe.shoppingservice.feign;

import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.shoppingservice.feign.dto.SellerProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "shopping-seller-service", url = "${feign.shopping-seller-service.url}", path = "/internal/products")
public interface SellerProductClient {

    @GetMapping("/{productId}")
    ApiResponse<SellerProductResponse> getProduct(@PathVariable("productId") Long productId);

    @GetMapping
    ApiResponse<Object> getProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);
}
