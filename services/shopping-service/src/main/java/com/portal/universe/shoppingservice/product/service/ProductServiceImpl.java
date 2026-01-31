package com.portal.universe.shoppingservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.dto.*;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.shoppingservice.feign.BlogServiceClient;
import com.portal.universe.shoppingservice.feign.dto.BlogResponse;
import com.portal.universe.shoppingservice.inventory.service.InventoryService;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ProductService 인터페이스의 구현 클래스입니다.
 * 상품 관련 비즈니스 로직을 실제로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BlogServiceClient blogServiceClient;
    private final InventoryService inventoryService;

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(this::convertToResponse);
    }

    @Override
    @Transactional
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
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        // 1. 수정할 상품을 ID로 조회합니다.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 2. 상품 정보를 수정합니다.
        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                product.getImageUrl(),
                product.getCategory()
        );

        // 3. Dirty Checking으로 자동 저장 (save 호출 생략)
        return convertToResponse(product);
    }

    @Override
    @Transactional
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
                product.getImageUrl(),
                product.getCategory(),
                reviews
        );
    }

    // ========================================
    // Admin 전용 메서드
    // ========================================

    @Override
    @Transactional
    public ProductResponse createProductAdmin(AdminProductRequest request) {
        // 중복된 상품명 체크 (비즈니스 규칙 - DTO validation과 별개)
        if (productRepository.existsByName(request.name())) {
            throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
        }

        // DTO의 Jakarta Validation(@Positive, @Min)이 가격/재고 검증을 처리함

        Product newProduct = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .imageUrl(request.imageUrl())
                .category(request.category())
                .build();

        Product savedProduct = productRepository.save(newProduct);

        // 재고 시스템 자동 초기화
        if (request.stock() > 0) {
            try {
                inventoryService.initializeInventory(savedProduct.getId(), request.stock(), "SYSTEM");
                log.info("Inventory initialized for product: productId={}, stock={}", savedProduct.getId(), request.stock());
            } catch (Exception e) {
                log.warn("Failed to initialize inventory for product: productId={}, reason={}", savedProduct.getId(), e.getMessage());
            }
        }

        return convertToResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductAdmin(Long productId, AdminProductRequest request) {
        // 1. 수정할 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 2. 상품명 변경된 경우에만 중복 체크 (N+1 방지)
        if (!product.getName().equals(request.name())) {
            if (productRepository.existsByName(request.name())) {
                throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
            }
        }

        // 3. 상품 정보 수정
        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.imageUrl(),
                request.category()
        );

        // Dirty Checking으로 자동 저장 (save 호출 생략 가능하나 명시적으로 호출)
        return convertToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStock(Long productId, StockUpdateRequest request) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND));

        // 2. 재고만 업데이트 (Dirty Checking)
        product.update(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                request.stock(),
                product.getImageUrl(),
                product.getCategory()
        );

        return convertToResponse(product);
    }

    /**
     * Product 엔티티 객체를 ProductResponse DTO로 변환하는 헬퍼 메서드입니다.
     * @param product 변환할 Product 엔티티
     * @return 변환된 ProductResponse DTO
     */
    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(), product.getStock(), product.getImageUrl(), product.getCategory());
    }

}
