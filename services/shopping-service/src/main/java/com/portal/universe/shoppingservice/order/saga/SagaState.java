package com.portal.universe.shoppingservice.order.saga;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Saga 실행 상태를 추적하는 JPA 엔티티입니다.
 */
@Entity
@Table(name = "saga_states", indexes = {
        @Index(name = "idx_saga_order_id", columnList = "order_id"),
        @Index(name = "idx_saga_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id", nullable = false, unique = true, length = 50)
    private String sagaId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 30)
    private SagaStep currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SagaStatus status;

    /**
     * 완료된 단계들 (DB에는 CSV 문자열로 저장)
     */
    @Convert(converter = SagaStepSetConverter.class)
    @Column(name = "completed_steps", length = 500)
    private Set<SagaStep> completedSteps;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "compensation_attempts", nullable = false)
    private Integer compensationAttempts;

    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder
    public SagaState(Long orderId, String orderNumber) {
        this.sagaId = generateSagaId();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.currentStep = SagaStep.RESERVE_INVENTORY;
        this.status = SagaStatus.STARTED;
        this.completedSteps = EnumSet.noneOf(SagaStep.class);
        this.compensationAttempts = 0;
    }

    public void proceedToNextStep() {
        this.completedSteps.add(this.currentStep);

        SagaStep nextStep = this.currentStep.next();
        if (nextStep != null) {
            this.currentStep = nextStep;
        }
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.completedSteps.add(this.currentStep);
    }

    public void startCompensation(String errorMessage) {
        this.status = SagaStatus.COMPENSATING;
        this.lastErrorMessage = errorMessage;
    }

    public void markAsFailed(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.lastErrorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void markAsCompensationFailed(String errorMessage) {
        this.status = SagaStatus.COMPENSATION_FAILED;
        this.lastErrorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void incrementCompensationAttempts() {
        this.compensationAttempts++;
    }

    public boolean isStepCompleted(SagaStep step) {
        return this.completedSteps.contains(step);
    }

    private static String generateSagaId() {
        return "SAGA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
