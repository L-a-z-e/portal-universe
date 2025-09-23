package com.portal.universe.shoppingservice.service;

import com.portal.universe.shoppingservice.dto.ProductCreateRequest;
import com.portal.universe.shoppingservice.dto.ProductResponse;
import com.portal.universe.shoppingservice.dto.ProductUpdateRequest;
import com.portal.universe.shoppingservice.dto.ProductWithReviewsResponse;

public interface ProductService {
    ProductResponse createProduct(ProductCreateRequest request);
    ProductResponse getProductById(Long id);
    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);
    void deleteProduct(Long productId);
    ProductWithReviewsResponse getProductWithReviews(Long productId);
}
