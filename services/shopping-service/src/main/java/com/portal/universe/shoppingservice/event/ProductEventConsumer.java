package com.portal.universe.shoppingservice.event;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.shoppingservice.common.config.CacheType;
import com.portal.universe.shoppingservice.common.exception.ShoppingErrorCode;
import com.portal.universe.event.seller.ProductCreatedEvent;
import com.portal.universe.event.seller.ProductDeletedEvent;
import com.portal.universe.event.seller.ProductUpdatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import com.portal.universe.shoppingservice.search.document.ProductDocument;
import com.portal.universe.shoppingservice.search.outbox.EsOutboxEvent;
import com.portal.universe.shoppingservice.search.outbox.EsOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private static final String ES_INDEX_PRODUCTS = "products";

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final EsOutboxEventRepository esOutboxEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = SellerTopics.PRODUCT_CREATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    @CacheEvict(value = CacheType.Names.PRODUCT_CATEGORIES, allEntries = true)
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreatedEvent: productId={}, sellerId={}",
                event.getProductId(), event.getSellerId());

        if (productRepository.existsById(event.getProductId())) {
            log.warn("Product already exists for id={}, skipping", event.getProductId());
            return;
        }

        Product product = Product.builder()
                .id(event.getProductId())
                .sellerId(event.getSellerId())
                .name(event.getName())
                .description(event.getDescription())
                .price(event.getPrice())
                .discountPrice(event.getDiscountPrice())
                .imageUrl(event.getImageUrl())
                .category(event.getCategory())
                .featured(event.getFeatured())
                .build();

        Product saved = productRepository.save(product);
        saveEsOutboxIndex(saved);

        // Read model 초기화: seller-service에서 InventoryEvent 수신 전까지 기본값 사용
        if (!inventoryRepository.existsByProductId(saved.getId())) {
            Inventory inventory = Inventory.builder()
                    .productId(saved.getId())
                    .initialQuantity(0)
                    .build();
            inventoryRepository.save(inventory);
        }

        log.info("Synced product and inventory from seller-service: id={}, name={}", saved.getId(), saved.getName());
    }

    @KafkaListener(topics = SellerTopics.PRODUCT_UPDATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheType.Names.PRODUCT_DETAIL, key = "#event.productId"),
            @CacheEvict(value = CacheType.Names.PRODUCT_CATEGORIES, allEntries = true)
    })
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("Received ProductUpdatedEvent: productId={}, sellerId={}",
                event.getProductId(), event.getSellerId());

        Product product = productRepository.findById(event.getProductId()).orElse(null);

        if (product == null) {
            product = Product.builder()
                    .id(event.getProductId())
                    .sellerId(event.getSellerId())
                    .name(event.getName())
                    .description(event.getDescription())
                    .price(event.getPrice())
                    .discountPrice(event.getDiscountPrice())
                    .imageUrl(event.getImageUrl())
                    .category(event.getCategory())
                    .featured(event.getFeatured())
                    .build();
            productRepository.save(product);
            log.info("Upserted product from ProductUpdatedEvent: id={}", event.getProductId());
        } else {
            product.updateFromSource(
                    event.getName(),
                    event.getDescription(),
                    event.getPrice(),
                    event.getDiscountPrice(),
                    event.getImageUrl(),
                    event.getCategory(),
                    event.getFeatured()
            );
            log.info("Updated product from seller-service: id={}", event.getProductId());
        }

        saveEsOutboxIndex(product);
    }

    @KafkaListener(topics = SellerTopics.PRODUCT_DELETED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheType.Names.PRODUCT_DETAIL, key = "#event.productId"),
            @CacheEvict(value = CacheType.Names.PRODUCT_CATEGORIES, allEntries = true)
    })
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Received ProductDeletedEvent: productId={}, sellerId={}",
                event.getProductId(), event.getSellerId());

        productRepository.findById(event.getProductId())
                .ifPresentOrElse(
                        product -> {
                            inventoryRepository.deleteByProductId(event.getProductId());
                            productRepository.delete(product);
                            esOutboxEventRepository.save(
                                    EsOutboxEvent.delete(ES_INDEX_PRODUCTS, String.valueOf(event.getProductId())));
                            log.info("Deleted product and inventory: id={}", event.getProductId());
                        },
                        () -> log.warn("Product not found for id={}, skipping delete",
                                event.getProductId())
                );
    }

    private void saveEsOutboxIndex(Product product) {
        try {
            String payload = objectMapper.writeValueAsString(ProductDocument.from(product));
            esOutboxEventRepository.save(
                    EsOutboxEvent.index(ES_INDEX_PRODUCTS, String.valueOf(product.getId()), payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize product for ES outbox: id={}", product.getId(), e);
            throw new CustomBusinessException(ShoppingErrorCode.ES_OUTBOX_SERIALIZATION_FAILED);
        }
    }
}
