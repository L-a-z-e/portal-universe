package com.portal.universe.shoppingservice.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

/**
 * 쇼핑 서비스 Seed Data 초기화.
 * Product, Inventory, Coupon은 seller-service에서 CQRS 이벤트로 동기화된다.
 * - ProductEventConsumer: Product read model + Inventory 생성
 * - CouponEventConsumer: Coupon read model + Redis 재고 초기화
 */
@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    @Profile({"local", "docker"})
    public CommandLineRunner initShoppingData() {
        return args -> log.info("Shopping service seed data is synced from seller-service via CQRS events");
    }
}
