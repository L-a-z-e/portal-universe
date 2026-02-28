package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.seller.SellerTopics;
import com.portal.universe.event.seller.TimeDealCancelledEvent;
import com.portal.universe.event.seller.TimeDealCreatedEvent;
import com.portal.universe.event.seller.TimeDealProductInfo;
import com.portal.universe.event.seller.TimeDealUpdatedEvent;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
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
public class TimeDealEventConsumer {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealRedisService timeDealRedisService;

    @KafkaListener(topics = SellerTopics.TIMEDEAL_CREATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onTimeDealCreated(TimeDealCreatedEvent event) {
        log.info("Received TimeDealCreatedEvent: timeDealId={}, sellerId={}",
                event.getTimeDealId(), event.getSellerId());

        if (timeDealRepository.existsBySourceTimeDealId(event.getTimeDealId())) {
            log.warn("TimeDeal already exists for sourceTimeDealId={}, skipping", event.getTimeDealId());
            return;
        }

        TimeDeal timeDeal = TimeDeal.builder()
                .sourceTimeDealId(event.getTimeDealId())
                .sellerId(event.getSellerId())
                .name(event.getName())
                .description(event.getDescription())
                .startsAt(event.getStartsAt())
                .endsAt(event.getEndsAt())
                .build();

        for (TimeDealProductInfo productInfo : event.getProducts()) {
            TimeDealProduct tdp = TimeDealProduct.builder()
                    .productId(productInfo.getProductId())
                    .dealPrice(productInfo.getDealPrice())
                    .dealQuantity(productInfo.getDealQuantity())
                    .maxPerUser(productInfo.getMaxPerUser())
                    .build();
            timeDeal.addProduct(tdp);
        }

        TimeDeal saved = timeDealRepository.save(timeDeal);

        // Redis 재고 초기화
        long ttlSeconds = Duration.between(Instant.now(), saved.getEndsAt()).getSeconds()
                + TimeUnit.DAYS.toSeconds(1);
        for (TimeDealProduct tdp : saved.getProducts()) {
            if (ttlSeconds > 0) {
                timeDealRedisService.initializeStock(
                        saved.getId(), tdp.getProductId(), tdp.getDealQuantity(), ttlSeconds);
            } else {
                timeDealRedisService.initializeStock(
                        saved.getId(), tdp.getProductId(), tdp.getDealQuantity());
            }
        }

        log.info("Synced time deal from seller-service: id={}, sourceTimeDealId={}, products={}",
                saved.getId(), saved.getSourceTimeDealId(), saved.getProducts().size());
    }

    @KafkaListener(topics = SellerTopics.TIMEDEAL_UPDATED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onTimeDealUpdated(TimeDealUpdatedEvent event) {
        log.info("Received TimeDealUpdatedEvent: timeDealId={}, sellerId={}",
                event.getTimeDealId(), event.getSellerId());

        TimeDeal timeDeal = timeDealRepository.findBySourceTimeDealId(event.getTimeDealId())
                .orElse(null);

        if (timeDeal == null) {
            // Upsert: 없으면 생성
            timeDeal = TimeDeal.builder()
                    .sourceTimeDealId(event.getTimeDealId())
                    .sellerId(event.getSellerId())
                    .name(event.getName())
                    .description(event.getDescription())
                    .startsAt(event.getStartsAt())
                    .endsAt(event.getEndsAt())
                    .build();

            for (TimeDealProductInfo productInfo : event.getProducts()) {
                TimeDealProduct tdp = TimeDealProduct.builder()
                        .productId(productInfo.getProductId())
                        .dealPrice(productInfo.getDealPrice())
                        .dealQuantity(productInfo.getDealQuantity())
                        .maxPerUser(productInfo.getMaxPerUser())
                        .build();
                timeDeal.addProduct(tdp);
            }
            timeDealRepository.save(timeDeal);
            log.info("Upserted time deal from TimeDealUpdatedEvent: sourceTimeDealId={}",
                    event.getTimeDealId());
        } else {
            timeDeal.updateFromSource(
                    event.getName(),
                    event.getDescription(),
                    event.getStartsAt(),
                    event.getEndsAt(),
                    TimeDealStatus.valueOf(event.getStatus())
            );
            log.info("Updated time deal from seller-service: sourceTimeDealId={}",
                    event.getTimeDealId());
        }
    }

    @KafkaListener(topics = SellerTopics.TIMEDEAL_CANCELLED, groupId = "shopping-service",
            containerFactory = "avroKafkaListenerContainerFactory")
    @Transactional
    public void onTimeDealCancelled(TimeDealCancelledEvent event) {
        log.info("Received TimeDealCancelledEvent: timeDealId={}, sellerId={}",
                event.getTimeDealId(), event.getSellerId());

        timeDealRepository.findBySourceTimeDealId(event.getTimeDealId())
                .ifPresentOrElse(
                        timeDeal -> {
                            timeDeal.cancel();
                            timeDeal.getProducts().forEach(tdp ->
                                    timeDealRedisService.deleteTimeDealCache(
                                            timeDeal.getId(), tdp.getProductId()));
                            log.info("Cancelled time deal: sourceTimeDealId={}", event.getTimeDealId());
                        },
                        () -> log.warn("TimeDeal not found for sourceTimeDealId={}, skipping cancel",
                                event.getTimeDealId())
                );
    }
}
