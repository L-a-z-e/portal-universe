package com.portal.universe.shoppingsellerservice.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.shoppingsellerservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingsellerservice.coupon.dto.CouponCreateRequest;
import com.portal.universe.shoppingsellerservice.coupon.repository.CouponRepository;
import com.portal.universe.shoppingsellerservice.coupon.service.CouponService;
import com.portal.universe.shoppingsellerservice.inventory.repository.InventoryRepository;
import com.portal.universe.shoppingsellerservice.inventory.service.InventoryService;
import com.portal.universe.shoppingsellerservice.product.domain.Product;
import com.portal.universe.shoppingsellerservice.product.dto.ProductCreateRequest;
import com.portal.universe.shoppingsellerservice.product.repository.ProductRepository;
import com.portal.universe.shoppingsellerservice.product.service.ProductService;
import com.portal.universe.shoppingsellerservice.seller.domain.Seller;
import com.portal.universe.shoppingsellerservice.seller.repository.SellerRepository;
import com.portal.universe.shoppingsellerservice.timedeal.dto.TimeDealCreateRequest;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
import com.portal.universe.shoppingsellerservice.timedeal.service.TimeDealService;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final SellerRepository sellerRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final TimeDealService timeDealService;
    private final TimeDealRepository timeDealRepository;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(1)
    @Profile({"local", "docker", "kubernetes"})
    public CommandLineRunner initSellerData() {
        return args -> {
            if (sellerRepository.count() > 0) {
                if (inventoryRepository.count() == 0 && productRepository.count() > 0) {
                    log.info("Products exist but inventory missing, creating inventory...");
                    createInventory();
                }
                if (timeDealRepository.count() == 0 && productRepository.count() > 0) {
                    log.info("Products exist but time deals missing, creating time deals...");
                    createTimeDeals();
                }
                // Check if new coupons (e.g. FLASH) need to be added
                createCoupons();
                log.info("Seller seed data already exists, skipping");
                return;
            }

            log.info("Initializing seller seed data...");
            initializeData();
            log.info("Seller seed data initialization completed");
        };
    }

    @Transactional
    public void initializeData() throws IOException {
        createSeller();
        createProducts();
        createInventory();
        createCoupons();
        createTimeDeals();
    }

    private void createSeller() {
        Seller seller = Seller.builder()
                .userId("00000000-0000-0000-0000-000000000001")
                .businessName("Portal Test Store")
                .businessNumber("123-45-67890")
                .representativeName("테스트판매자")
                .phone("010-0000-0000")
                .email("seller@test.com")
                .bankName("테스트은행")
                .bankAccount("1234-5678-9012")
                .commissionRate(new BigDecimal("10.00"))
                .build();
        seller.approve("system", "DataInitializer seed data");
        sellerRepository.save(seller);
        log.info("Created test seller: {}", seller.getBusinessName());
    }

    private void createProducts() throws IOException {
        List<ProductSeed> seeds = readSeed("products.json", ProductSeed.class);
        Seller seller = sellerRepository.findAll().get(0);

        for (ProductSeed seed : seeds) {
            ProductCreateRequest request = new ProductCreateRequest(
                    seed.name(), seed.description(), seed.price(), seed.discountPrice(),
                    seed.stock(), seed.imageUrl(), seed.category(), seed.featured()
            );
            productService.createProduct(seller.getId(), request);
        }
        log.info("Created {} products for seller {} (events published)", seeds.size(), seller.getBusinessName());
    }

    private void createInventory() {
        List<Product> products = productRepository.findAll();
        int created = 0;
        for (Product product : products) {
            if (inventoryRepository.findByProductId(product.getId()).isEmpty()) {
                int quantity = product.getStock() != null ? product.getStock() : 100;
                inventoryService.initializeInventory(product.getId(), quantity);
                created++;
            }
        }
        log.info("Created inventory for {} products (events published)", created);
    }

    private void createCoupons() throws IOException {
        long existingCount = couponRepository.count();
        List<CouponSeed> seeds = readSeed("coupons.json", CouponSeed.class);

        if (existingCount >= seeds.size()) {
            log.info("Coupon seed data already exists ({}/{}), skipping", existingCount, seeds.size());
            return;
        }

        Seller seller = sellerRepository.findAll().get(0);
        Instant now = Instant.now();

        // Skip already existing coupons, create only missing ones
        List<CouponSeed> toCreate = seeds.subList((int) existingCount, seeds.size());
        log.info("Creating {} additional coupons (existing: {})", toCreate.size(), existingCount);

        for (CouponSeed seed : toCreate) {
            CouponCreateRequest request = new CouponCreateRequest(
                    seed.code(), seed.name(), seed.description(),
                    DiscountType.valueOf(seed.discountType()),
                    seed.discountValue(), seed.minimumOrderAmount(), seed.maximumDiscountAmount(),
                    seed.totalQuantity(),
                    now.plus(seed.daysAfterBase(), ChronoUnit.DAYS),
                    now.plus(seed.daysAfterBase() + seed.durationDays(), ChronoUnit.DAYS)
            );
            couponService.createCoupon(seller.getId(), request);
        }
        log.info("Created {} coupons for seller {} (events published)", seeds.size(), seller.getBusinessName());
    }

    private void createTimeDeals() throws IOException {
        if (timeDealRepository.count() > 0) {
            log.info("TimeDeal seed data already exists, skipping");
            return;
        }

        List<TimeDealSeed> seeds = readSeed("time-deals.json", TimeDealSeed.class);
        Seller seller = sellerRepository.findAll().get(0);
        List<Product> allProducts = productRepository.findAll();
        Instant now = Instant.now();

        for (TimeDealSeed seed : seeds) {
            Instant startsAt = now.plus(seed.hoursFromNow(), ChronoUnit.HOURS);
            Instant endsAt = startsAt.plus(seed.durationHours(), ChronoUnit.HOURS);

            List<TimeDealCreateRequest.TimeDealProductItem> items = seed.products().stream()
                    .filter(p -> p.productIndex() < allProducts.size())
                    .map(p -> new TimeDealCreateRequest.TimeDealProductItem(
                            allProducts.get(p.productIndex()).getId(),
                            p.dealPrice(),
                            p.dealQuantity(),
                            p.maxPerUser()
                    ))
                    .toList();

            TimeDealCreateRequest request = new TimeDealCreateRequest(
                    seed.name(), seed.description(), startsAt, endsAt, items
            );
            timeDealService.createTimeDeal(seller.getId(), request);
        }
        log.info("Created {} time deals for seller {} (events published)", seeds.size(), seller.getBusinessName());
    }

    private <T> List<T> readSeed(String filename, Class<T> type) throws IOException {
        Resource resource = new ClassPathResource("seed/" + filename);
        return objectMapper.readValue(resource.getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductSeed(String name, String description, BigDecimal price, BigDecimal discountPrice,
                       Integer stock, String imageUrl, String category, Boolean featured) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CouponSeed(String code, String name, String description, String discountType,
                      BigDecimal discountValue, BigDecimal minimumOrderAmount, BigDecimal maximumDiscountAmount,
                      Integer totalQuantity, int daysAfterBase, int durationDays) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TimeDealSeed(String name, String description, String status,
                        int hoursFromNow, int durationHours,
                        List<TimeDealProductSeed> products) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TimeDealProductSeed(int productIndex, BigDecimal dealPrice,
                               Integer dealQuantity, Integer maxPerUser) {}
}
