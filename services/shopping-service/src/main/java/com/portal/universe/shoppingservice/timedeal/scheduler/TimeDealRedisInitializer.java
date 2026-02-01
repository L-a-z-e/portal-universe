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

import java.util.List;

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

            dealWithProducts.getProducts().forEach(product -> {
                int remainingQuantity = product.getDealQuantity() - product.getSoldQuantity();
                if (remainingQuantity > 0) {
                    timeDealRedisService.initializeStock(
                            deal.getId(),
                            product.getProduct().getId(),
                            remainingQuantity
                    );
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
