package com.portal.universe.shoppingsellerservice.product.service;

import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse createProduct(Long sellerId, ProductCreateRequest request);
    ProductResponse getProduct(Long productId);
    Page<ProductResponse> getSellerProducts(Long sellerId, Pageable pageable);
    Page<ProductResponse> getAllProducts(Pageable pageable);
    ProductResponse updateProduct(Long sellerId, Long productId, ProductUpdateRequest request);
    void deleteProduct(Long sellerId, Long productId);
}
