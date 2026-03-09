package com.portal.universe.shoppingsellerservice.product.service;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.seller.ProductCreatedEvent;
import com.portal.universe.event.seller.ProductDeletedEvent;
import com.portal.universe.event.seller.ProductUpdatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.shoppingsellerservice.common.exception.SellerErrorCode;
import com.portal.universe.shoppingsellerservice.event.outbox.OutboxEvent;
import com.portal.universe.shoppingsellerservice.event.outbox.OutboxEventRepository;
import com.portal.universe.shoppingsellerservice.product.domain.Product;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.dto.ProductResponse;
import com.portal.universe.shoppingsellerservice.product.dto.ProductUpdateRequest;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final MeterRegistry meterRegistry;

    private Counter productCreatedCounter;

    @PostConstruct
    void initMetrics() {
        productCreatedCounter = Counter.builder("product_created_total")
                .description("Total products created")
                .register(meterRegistry);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductCreateRequest request) {
        Product product = request.toEntity(sellerId);
        Product saved = productRepository.save(product);

        outboxEventRepository.save(OutboxEvent.create(
                SellerTopics.PRODUCT_CREATED,
                String.valueOf(saved.getId()),
                buildCreatedEvent(saved)));

        productCreatedCounter.increment();
        return ProductResponse.from(saved);
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
        product.updateDiscountPrice(request.discountPrice());
        if (request.featured() != null) {
            product.updateFeatured(request.featured());
        }

        outboxEventRepository.save(OutboxEvent.create(
                SellerTopics.PRODUCT_UPDATED,
                String.valueOf(product.getId()),
                buildUpdatedEvent(product)));

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

        outboxEventRepository.save(OutboxEvent.create(
                SellerTopics.PRODUCT_DELETED,
                String.valueOf(product.getId()),
                ProductDeletedEvent.newBuilder()
                        .setProductId(product.getId())
                        .setSellerId(product.getSellerId())
                        .setTimestamp(Instant.now())
                        .build()));
    }

    private ProductCreatedEvent buildCreatedEvent(Product product) {
        return ProductCreatedEvent.newBuilder()
                .setProductId(product.getId())
                .setSellerId(product.getSellerId())
                .setName(product.getName())
                .setDescription(product.getDescription())
                .setPrice(product.getPrice())
                .setDiscountPrice(product.getDiscountPrice())
                .setImageUrl(product.getImageUrl())
                .setCategory(product.getCategory())
                .setFeatured(product.getFeatured())
                .setTimestamp(Instant.now())
                .build();
    }

    private ProductUpdatedEvent buildUpdatedEvent(Product product) {
        return ProductUpdatedEvent.newBuilder()
                .setProductId(product.getId())
                .setSellerId(product.getSellerId())
                .setName(product.getName())
                .setDescription(product.getDescription())
                .setPrice(product.getPrice())
                .setDiscountPrice(product.getDiscountPrice())
                .setImageUrl(product.getImageUrl())
                .setCategory(product.getCategory())
                .setFeatured(product.getFeatured())
                .setTimestamp(Instant.now())
                .build();
    }
}
