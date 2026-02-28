package com.portal.universe.shoppingservice.order.saga;

import com.portal.universe.shoppingservice.event.CloudWatchMetricsPublisher;
import com.portal.universe.shoppingservice.order.domain.Order;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.order.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * STARTED/COMPENSATING 상태로 멈춘 Dead Saga를 주기적으로 탐지하여 보상 처리합니다.
 *
 * 서버 크래시, 타임아웃 등으로 Saga가 중간에 멈추면
 * 재고가 예약된 채 영구 잠금됩니다. 이 스케줄러가 이를 복구합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadSagaRecoveryScheduler {

    private static final int DEAD_SAGA_TIMEOUT_MINUTES = 30;

    private final SagaStateRepository sagaStateRepository;
    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final CloudWatchMetricsPublisher cloudWatchMetricsPublisher;

    /**
     * 5분마다 Dead Saga를 탐지하여 보상 처리합니다.
     * FOR UPDATE SKIP LOCKED로 다중 인스턴스 중복 처리를 방지합니다.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void recoverDeadSagas() {
        Instant cutoff = Instant.now().minus(DEAD_SAGA_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        List<SagaStatus> targetStatuses = List.of(SagaStatus.STARTED, SagaStatus.COMPENSATING);

        List<SagaState> deadSagas = sagaStateRepository.findDeadSagas(targetStatuses, cutoff);

        if (deadSagas.isEmpty()) {
            return;
        }

        log.warn("Found {} dead saga(s) to recover", deadSagas.size());

        for (SagaState sagaState : deadSagas) {
            recoverSaga(sagaState);
        }
    }

    private void recoverSaga(SagaState sagaState) {
        String sagaId = sagaState.getSagaId();
        String orderNumber = sagaState.getOrderNumber();

        log.info("Recovering dead saga: {} (order: {}, status: {}, started: {})",
                sagaId, orderNumber, sagaState.getStatus(), sagaState.getStartedAt());

        try {
            orderSagaOrchestrator.compensate(sagaState, "Dead saga recovery (timeout)");

            cloudWatchMetricsPublisher.publishOrderFailure(orderNumber, "DEAD_SAGA_RECOVERY");
            log.info("Dead saga recovered: {} (order: {})", sagaId, orderNumber);

        } catch (Exception e) {
            log.error("Failed to recover dead saga: {} (order: {}): {}",
                    sagaId, orderNumber, e.getMessage());

            if (sagaState.getStatus() == SagaStatus.COMPENSATION_FAILED) {
                cloudWatchMetricsPublisher.publishOrderFailure(orderNumber, "DEAD_SAGA_COMPENSATION_FAILED");
                log.error("Dead saga requires manual intervention: {} (order: {})", sagaId, orderNumber);
            }
        }
    }
}
