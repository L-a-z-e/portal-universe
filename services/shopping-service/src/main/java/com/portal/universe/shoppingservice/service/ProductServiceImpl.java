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

/**
 * ProductService 인터페이스의 구현 클래스입니다.
 * 상품 관련 비즈니스 로직을 실제로 처리합니다.
 */
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        return convertToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        // 1. 수정할 상품을 ID로 조회합니다.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 2. 상품 정보를 수정합니다.
        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock()
        );

        // 3. 수정된 상품을 저장하고 응답 DTO로 변환하여 반환합니다.
        Product updatedProduct = productRepository.save(product);
        return convertToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long productId) {
        // 삭제할 상품이 존재하는지 먼저 확인합니다.
        if (!productRepository.existsById(productId)) {
            throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.deleteById(productId);
    }

    /**
     * 상품 정보와 해당 상품의 리뷰 목록을 함께 조회합니다.
     * 이 메서드는 서비스 오케스트레이션의 예시입니다.
     */
    @Override
    public ProductWithReviewsResponse getProductWithReviews(Long productId) {
        // 1. Shopping 서비스의 DB에서 상품 정보를 조회합니다.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 2. Feign 클라이언트를 통해 Blog 서비스의 API를 호출하여 리뷰 목록을 가져옵니다.
        List<BlogResponse> reviews = blogServiceClient.getPostByProductId(String.valueOf(productId));

        // 3. 두 데이터를 조합하여 최종 응답 DTO를 생성하고 반환합니다.
        return new ProductWithReviewsResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                reviews
        );
    }

    /**
     * Product 엔티티 객체를 ProductResponse DTO로 변환하는 헬퍼 메서드입니다.
     * @param product 변환할 Product 엔티티
     * @return 변환된 ProductResponse DTO
     */
    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getDescription(), product.getPrice(), product.getStock());
    }

}
