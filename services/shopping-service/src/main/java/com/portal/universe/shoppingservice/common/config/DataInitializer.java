package com.portal.universe.shoppingservice.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingservice.coupon.domain.*;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingservice.coupon.repository.UserCouponRepository;
import com.portal.universe.shoppingservice.inventory.domain.Inventory;
import com.portal.universe.shoppingservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingservice.product.domain.Product;
import com.portal.universe.shoppingservice.product.domain.ProductImage;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 쇼핑 서비스 Seed Data 초기화.
 * resources/seed/ 디렉토리의 JSON 파일에서 데이터를 로드한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    @Profile({"local", "docker"})
    public CommandLineRunner initShoppingData() {
        return args -> {
            if (productRepository.count() > 0) {
                if (inventoryRepository.count() == 0) {
                    log.info("Products exist but inventory missing, creating inventory...");
                    createInventory();
                } else {
                    log.info("Shopping seed data already exists, skipping");
                }
                return;
            }

            log.info("Initializing shopping seed data...");
            initializeData();
            log.info("Shopping seed data initialization completed");
        };
    }

    @Transactional
    public void initializeData() throws IOException {
        createProducts();
        createInventory();
        createCouponsAndAssignToTestUser();
    }

    private void createProducts() throws IOException {
        List<ProductSeed> seeds = readSeed("products.json", ProductSeed.class);
        for (ProductSeed seed : seeds) {
            Product product = Product.builder()
                    .name(seed.name())
                    .description(seed.description())
                    .price(seed.price())
                    .discountPrice(seed.discountPrice())
                    .stock(seed.stock())
                    .imageUrl(seed.imageUrl())
                    .category(seed.category())
                    .featured(seed.featured())
                    .build();

            if (seed.additionalImages() != null) {
                for (ImageSeed imageSeed : seed.additionalImages()) {
                    ProductImage productImage = ProductImage.builder()
                            .product(product)
                            .imageUrl(imageSeed.url())
                            .sortOrder(imageSeed.sortOrder())
                            .altText(imageSeed.altText())
                            .build();
                    product.getImages().add(productImage);
                }
            }
            productRepository.save(product);
        }
        log.info("Created {} products", seeds.size());
    }

    private void createInventory() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            if (!inventoryRepository.existsByProductId(product.getId())) {
                Inventory inventory = Inventory.builder()
                        .productId(product.getId())
                        .initialQuantity(product.getStock() != null ? product.getStock() : 100)
                        .build();
                inventoryRepository.save(inventory);
            }
        }
        log.info("Created inventory for {} products", products.size());
    }

    private void createCouponsAndAssignToTestUser() throws IOException {
        List<CouponSeed> seeds = readSeed("coupons.json", CouponSeed.class);
        LocalDateTime now = LocalDateTime.now();

        // auth-service의 고정 테스트 유저 UUID
        String testUserUuid = "00000000-0000-0000-0000-000000000001"; // test@test.com

        for (CouponSeed seed : seeds) {
            Coupon coupon = Coupon.builder()
                    .code(seed.code())
                    .name(seed.name())
                    .description(seed.description())
                    .discountType(DiscountType.valueOf(seed.discountType()))
                    .discountValue(seed.discountValue())
                    .minimumOrderAmount(seed.minimumOrderAmount())
                    .maximumDiscountAmount(seed.maximumDiscountAmount())
                    .totalQuantity(seed.totalQuantity())
                    .startsAt(now.plusDays(seed.daysAfterBase()))
                    .expiresAt(now.plusDays(seed.daysAfterBase() + seed.durationDays()))
                    .build();

            Coupon savedCoupon = couponRepository.save(coupon);

            // 테스트 유저에게 쿠폰 발급 (자동 발급 시나리오)
            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(testUserUuid)
                    .coupon(savedCoupon)
                    .expiresAt(savedCoupon.getExpiresAt())
                    .build();
            
            userCouponRepository.save(userCoupon);
            savedCoupon.incrementIssuedQuantity();
        }
        log.info("Created {} coupons and assigned to test user", seeds.size());
    }

    private <T> List<T> readSeed(String filename, Class<T> type) throws IOException {
        Resource resource = new ClassPathResource("seed/" + filename);
        return objectMapper.readValue(resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    // ========== Seed DTOs ==========

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductSeed(String name, String description, BigDecimal price, BigDecimal discountPrice,
                       Integer stock, String imageUrl, String category, Boolean featured,
                       List<ImageSeed> additionalImages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageSeed(String url, Integer sortOrder, String altText) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CouponSeed(String code, String name, String description, String discountType,
                      BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
                      Integer totalQuantity, int daysAfterBase, int durationDays) {}
}
