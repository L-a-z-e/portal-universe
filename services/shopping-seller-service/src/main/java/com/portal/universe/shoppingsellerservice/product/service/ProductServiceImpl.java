package com.portal.universe.shoppingsellerservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.product.domain.Product;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductCreateRequest request) {
        Product product = request.toEntity(sellerId);
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    @Override
    public Page<ProductResponse> getSellerProducts(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductResponse::from);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long sellerId, Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_FOUND));
        if (!product.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_OWNED);
        }
        product.update(request.name(), request.description(), request.price(),
                request.stock(), request.imageUrl(), request.category());
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long sellerId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_FOUND));
        if (!product.getSellerId().equals(sellerId)) {
            throw new CustomBusinessException(SellerErrorCode.PRODUCT_NOT_OWNED);
        }
        productRepository.delete(product);
    }
}
