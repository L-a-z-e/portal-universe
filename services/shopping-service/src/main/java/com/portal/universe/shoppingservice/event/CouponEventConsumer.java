package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.seller.CouponCreatedEvent;
import com.portal.universe.event.seller.CouponDeletedEvent;
import com.portal.universe.event.seller.CouponUpdatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.shoppingservice.coupon.domain.Coupon;
import com.portal.universe.shoppingservice.coupon.domain.CouponStatus;
import com.portal.universe.shoppingservice.coupon.domain.DiscountType;
import com.portal.universe.shoppingservice.coupon.redis.CouponRedisService;
import com.portal.universe.shoppingservice.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {

    private final CouponRepository couponRepository;
    private final CouponRedisService couponRedisService;

    @KafkaListener(topics = SellerTopics.COUPON_CREATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onCouponCreated(CouponCreatedEvent event) {
        log.info("Received CouponCreatedEvent: couponId={}, sellerId={}",
                event.getCouponId(), event.getSellerId());

        if (couponRepository.existsBySourceCouponId(event.getCouponId())) {
            log.warn("Coupon already exists for sourceCouponId={}, skipping", event.getCouponId());
            return;
        }

        Coupon coupon = Coupon.builder()
                .sourceCouponId(event.getCouponId())
                .sellerId(event.getSellerId())
                .code(event.getCode())
                .name(event.getName())
                .description(event.getDescription())
                .discountType(DiscountType.valueOf(event.getDiscountType()))
                .discountValue(event.getDiscountValue())
                .minimumOrderAmount(event.getMinimumOrderAmount())
                .maximumDiscountAmount(event.getMaximumDiscountAmount())
                .totalQuantity(event.getTotalQuantity())
                .startsAt(event.getStartsAt())
                .expiresAt(event.getExpiresAt())
                .build();

        Coupon saved = couponRepository.save(coupon);

        long ttlSeconds = Duration.between(Instant.now(), saved.getExpiresAt()).getSeconds()
                + TimeUnit.DAYS.toSeconds(1);
        if (ttlSeconds > 0) {
            couponRedisService.initializeCouponStock(saved.getId(), saved.getTotalQuantity(), ttlSeconds);
        } else {
            couponRedisService.initializeCouponStock(saved.getId(), saved.getTotalQuantity());
        }

        log.info("Synced coupon from seller-service: id={}, sourceCouponId={}, code={}",
                saved.getId(), saved.getSourceCouponId(), saved.getCode());
    }

    @KafkaListener(topics = SellerTopics.COUPON_UPDATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onCouponUpdated(CouponUpdatedEvent event) {
        log.info("Received CouponUpdatedEvent: couponId={}, sellerId={}",
                event.getCouponId(), event.getSellerId());

        Coupon coupon = couponRepository.findBySourceCouponId(event.getCouponId())
                .orElse(null);

        if (coupon == null) {
            // Upsert: 없으면 생성
            coupon = Coupon.builder()
                    .sourceCouponId(event.getCouponId())
                    .sellerId(event.getSellerId())
                    .code(event.getCode())
                    .name(event.getName())
                    .description(event.getDescription())
                    .discountType(DiscountType.valueOf(event.getDiscountType()))
                    .discountValue(event.getDiscountValue())
                    .minimumOrderAmount(event.getMinimumOrderAmount())
                    .maximumDiscountAmount(event.getMaximumDiscountAmount())
                    .totalQuantity(event.getTotalQuantity())
                    .startsAt(event.getStartsAt())
                    .expiresAt(event.getExpiresAt())
                    .build();
            couponRepository.save(coupon);
            log.info("Upserted coupon from CouponUpdatedEvent: sourceCouponId={}", event.getCouponId());
        } else {
            coupon.updateFromSource(
                    event.getName(),
                    event.getDescription(),
                    DiscountType.valueOf(event.getDiscountType()),
                    event.getDiscountValue(),
                    event.getMinimumOrderAmount(),
                    event.getMaximumDiscountAmount(),
                    event.getTotalQuantity(),
                    event.getStartsAt(),
                    event.getExpiresAt(),
                    CouponStatus.valueOf(event.getStatus())
            );
            log.info("Updated coupon from seller-service: sourceCouponId={}", event.getCouponId());
        }
    }

    @KafkaListener(topics = SellerTopics.COUPON_DELETED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onCouponDeleted(CouponDeletedEvent event) {
        log.info("Received CouponDeletedEvent: couponId={}, sellerId={}",
                event.getCouponId(), event.getSellerId());

        couponRepository.findBySourceCouponId(event.getCouponId())
                .ifPresentOrElse(
                        coupon -> {
                            coupon.deactivate();
                            couponRedisService.deleteCouponCache(coupon.getId());
                            log.info("Deactivated coupon: sourceCouponId={}", event.getCouponId());
                        },
                        () -> log.warn("Coupon not found for sourceCouponId={}, skipping delete",
                                event.getCouponId())
                );
    }
}
