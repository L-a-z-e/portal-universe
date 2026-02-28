package com.portal.universe.shoppingsellerservice.timedeal.scheduler;

import com.portal.universe.event.seller.TimeDealProductInfo;
import com.portal.universe.event.seller.TimeDealUpdatedEvent;
import com.portal.universe.shoppingsellerservice.common.annotation.DistributedLock;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.repository.TimeDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeDealScheduler {

    private final TimeDealRepository timeDealRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000)
    @DistributedLock(key = "'scheduler:seller:timedeal:status'", waitTime = 0, leaseTime = 55)
    @Transactional
    public void updateTimeDealStatus() {
        Instant now = Instant.now();
        log.debug("Running TimeDeal status update scheduler at {}", now);

        activateScheduledDeals(now);
        endActiveDeals(now);
    }

    private void activateScheduledDeals(Instant now) {
        List<TimeDeal> dealsToStart = timeDealRepository.findDealsToStart(now);
        if (dealsToStart.isEmpty()) return;

        dealsToStart.forEach(TimeDeal::activate);
        timeDealRepository.saveAll(dealsToStart);

        for (TimeDeal deal : dealsToStart) {
            publishUpdatedEvent(deal);
            log.info("Activated time deal: id={}, name={}", deal.getId(), deal.getName());
        }
    }

    private void endActiveDeals(Instant now) {
        List<TimeDeal> dealsToEnd = timeDealRepository.findDealsToEnd(now);
        if (dealsToEnd.isEmpty()) return;

        dealsToEnd.forEach(TimeDeal::end);
        timeDealRepository.saveAll(dealsToEnd);

        for (TimeDeal deal : dealsToEnd) {
            publishUpdatedEvent(deal);
            log.info("Ended time deal: id={}, name={}", deal.getId(), deal.getName());
        }
    }

    private void publishUpdatedEvent(TimeDeal deal) {
        List<TimeDealProductInfo> productInfos = deal.getProducts().stream()
                .map(tdp -> TimeDealProductInfo.newBuilder()
                        .setProductId(tdp.getProductId())
                        .setDealPrice(tdp.getDealPrice())
                        .setDealQuantity(tdp.getDealQuantity())
                        .setMaxPerUser(tdp.getMaxPerUser())
                        .build())
                .toList();

        eventPublisher.publishEvent(TimeDealUpdatedEvent.newBuilder()
                .setTimeDealId(deal.getId())
                .setSellerId(deal.getSellerId())
                .setName(deal.getName())
                .setDescription(deal.getDescription())
                .setStartsAt(deal.getStartsAt())
                .setEndsAt(deal.getEndsAt())
                .setStatus(deal.getStatus().name())
                .setProducts(productInfos)
                .setTimestamp(Instant.now())
                .build());
    }
}
