package com.portal.universe.shoppingservice.timedeal.scheduler;

import com.portal.universe.shoppingservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.redis.TimeDealRedisService;
import com.portal.universe.shoppingservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealRedisService timeDealRedisService;

    /**
     * 1분마다 타임딜 상태를 체크하여 업데이트합니다.
     * 분산 락을 사용하여 중복 실행을 방지합니다.
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    @DistributedLock(key = "'scheduler:timedeal:status'", waitTime = 0, leaseTime = 55)
    @Transactional
    public void updateTimeDealStatus() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Running TimeDeal status update scheduler at {}", now);

        activateScheduledDeals(now);
        endActiveDeals(now);
    }

    private void activateScheduledDeals(LocalDateTime now) {
        List<TimeDeal> dealsToStart = timeDealRepository.findDealsToStart(now);

        for (TimeDeal deal : dealsToStart) {
            deal.activate();
            timeDealRepository.save(deal);

            // Redis에 재고 초기화
            deal.getProducts().forEach(product ->
                    timeDealRedisService.initializeStock(
                            deal.getId(),
                            product.getProduct().getId(),
                            product.getDealQuantity()
                    )
            );

            log.info("Activated time deal: id={}, name={}", deal.getId(), deal.getName());
        }
    }

    private void endActiveDeals(LocalDateTime now) {
        List<TimeDeal> dealsToEnd = timeDealRepository.findDealsToEnd(now);

        for (TimeDeal deal : dealsToEnd) {
            deal.end();
            timeDealRepository.save(deal);

            // Redis 캐시 정리
            deal.getProducts().forEach(product ->
                    timeDealRedisService.deleteTimeDealCache(
                            deal.getId(),
                            product.getProduct().getId()
                    )
            );

            log.info("Ended time deal: id={}, name={}", deal.getId(), deal.getName());
        }
    }
}
