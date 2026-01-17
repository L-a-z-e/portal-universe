package com.portal.universe.shoppingservice.order.saga;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
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

    /**
     * Saga 고유 식별자
     */
    @Column(name = "saga_id", nullable = false, unique = true, length = 50)
    private String sagaId;

    /**
     * 주문 ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 주문 번호
     */
    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    /**
     * 현재 실행 단계
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 30)
    private SagaStep currentStep;

    /**
     * Saga 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SagaStatus status;

    /**
     * 완료된 단계들 (JSON 형태로 저장)
     */
    @Column(name = "completed_steps", length = 500)
    private String completedSteps;

    /**
     * 마지막 에러 메시지
     */
    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    /**
     * 보상 시도 횟수
     */
    @Column(name = "compensation_attempts", nullable = false)
    private Integer compensationAttempts;

    @CreatedDate
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    /**
     * 완료 일시
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    public SagaState(Long orderId, String orderNumber) {
        this.sagaId = generateSagaId();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.currentStep = SagaStep.RESERVE_INVENTORY;
        this.status = SagaStatus.STARTED;
        this.completedSteps = "";
        this.compensationAttempts = 0;
    }

    /**
     * 다음 단계로 진행합니다.
     */
    public void proceedToNextStep() {
        // 현재 단계를 완료된 단계 목록에 추가
        if (this.completedSteps.isEmpty()) {
            this.completedSteps = this.currentStep.name();
        } else {
            this.completedSteps += "," + this.currentStep.name();
        }

        SagaStep nextStep = this.currentStep.next();
        if (nextStep != null) {
            this.currentStep = nextStep;
        }
    }

    /**
     * Saga를 완료합니다.
     */
    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        // 마지막 단계도 완료된 단계에 추가
        if (!this.completedSteps.contains(this.currentStep.name())) {
            if (this.completedSteps.isEmpty()) {
                this.completedSteps = this.currentStep.name();
            } else {
                this.completedSteps += "," + this.currentStep.name();
            }
        }
    }

    /**
     * 보상(롤백)을 시작합니다.
     */
    public void startCompensation(String errorMessage) {
        this.status = SagaStatus.COMPENSATING;
        this.lastErrorMessage = errorMessage;
    }

    /**
     * 이전 단계로 롤백합니다.
     */
    public void rollbackToPreviousStep() {
        SagaStep previousStep = this.currentStep.previous();
        if (previousStep != null) {
            this.currentStep = previousStep;
        }
    }

    /**
     * Saga 실패를 기록합니다.
     */
    public void markAsFailed(String errorMessage) {
        this.status = SagaStatus.FAILED;
        this.lastErrorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 보상 실패를 기록합니다.
     */
    public void markAsCompensationFailed(String errorMessage) {
        this.status = SagaStatus.COMPENSATION_FAILED;
        this.lastErrorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 보상 시도 횟수를 증가시킵니다.
     */
    public void incrementCompensationAttempts() {
        this.compensationAttempts++;
    }

    /**
     * 특정 단계가 완료되었는지 확인합니다.
     */
    public boolean isStepCompleted(SagaStep step) {
        return this.completedSteps.contains(step.name());
    }

    /**
     * Saga ID를 생성합니다.
     */
    private static String generateSagaId() {
        return "SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
