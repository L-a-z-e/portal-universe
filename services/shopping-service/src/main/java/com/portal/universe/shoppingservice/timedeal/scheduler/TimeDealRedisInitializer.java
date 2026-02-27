package com.portal.universe.shoppingservice.timedeal.scheduler;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 서비스 시작 시 ACTIVE 타임딜의 Redis 재고를 복원합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealRedisInitializer {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealRedisService timeDealRedisService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initializeActiveDeals() {
        List<TimeDeal> activeDeals = timeDealRepository.findByStatus(TimeDealStatus.ACTIVE);

        for (TimeDeal deal : activeDeals) {
            TimeDeal dealWithProducts = timeDealRepository.findByIdWithProducts(deal.getId());
            if (dealWithProducts == null || dealWithProducts.getProducts() == null) continue;

            long ttlSeconds = Duration.between(LocalDateTime.now(), deal.getEndsAt()).getSeconds()
                    + TimeUnit.DAYS.toSeconds(1);

            dealWithProducts.getProducts().forEach(product -> {
                int remainingQuantity = product.getDealQuantity() - product.getSoldQuantity();
                if (remainingQuantity > 0) {
                    // Stock 키: SETEX로 값 + TTL 원자적 설정
                    if (ttlSeconds > 0) {
                        timeDealRedisService.initializeStock(
                                deal.getId(),
                                product.getProduct().getId(),
                                remainingQuantity,
                                ttlSeconds
                        );
                    } else {
                        timeDealRedisService.initializeStock(
                                deal.getId(),
                                product.getProduct().getId(),
                                remainingQuantity
                        );
                    }
                }
            });

            log.info("Restored Redis stock for active time deal: id={}, name={}",
                    deal.getId(), deal.getName());
        }

        if (!activeDeals.isEmpty()) {
            log.info("Initialized Redis stock for {} active time deals", activeDeals.size());
        }
    }
}
