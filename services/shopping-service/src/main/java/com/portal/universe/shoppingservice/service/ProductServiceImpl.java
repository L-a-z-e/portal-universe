package com.portal.universe.shoppingservice.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.domain.Product;
import com.portal.universe.shoppingservice.dto.*;
import com.portal.universe.shoppingservice.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.feign.BlogServiceClient;
import com.portal.universe.shoppingservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BlogServiceClient blogServiceClient;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product newProduct = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .build();

        Product savedProduct = productRepository.save(newProduct);

        return convertToResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        return convertToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        Product updatedProduct = productRepository.save(product);

        return convertToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
        }

        productRepository.deleteById(productId);
    }

    @Override
    public ProductWithReviewsResponse getProductWithReviews(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        List<BlogResponse> reviews = blogServiceClient.getPostByProductId(String.valueOf(productId));

        return new ProductWithReviewsResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                reviews
        );

    }

    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getDescription(), product.getPrice(), product.getStock());
    }

}