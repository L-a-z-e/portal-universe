package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.order.saga.SagaState;
import com.portal.universe.shoppingservice.order.saga.SagaStatus;
import com.portal.universe.shoppingservice.order.saga.SagaStep;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Set;

public final class SagaStateFixture {

    private SagaStateFixture() {}

    public static SagaState create() {
        return builder().build();
    }

    public static SagaStateBuilder builder() {
        return new SagaStateBuilder();
    }

    public static class SagaStateBuilder {
        private Long id = 1L;
        private Long orderId = 1L;
        private String orderNumber = "ORD-20260310-001";
        private SagaStep currentStep = SagaStep.RESERVE_INVENTORY;
        private SagaStatus status = SagaStatus.STARTED;
        private Set<SagaStep> completedSteps = EnumSet.noneOf(SagaStep.class);
        private int compensationAttempts = 0;

        public SagaStateBuilder id(Long id) { this.id = id; return this; }
        public SagaStateBuilder orderId(Long orderId) { this.orderId = orderId; return this; }
        public SagaStateBuilder orderNumber(String orderNumber) { this.orderNumber = orderNumber; return this; }
        public SagaStateBuilder currentStep(SagaStep currentStep) { this.currentStep = currentStep; return this; }
        public SagaStateBuilder status(SagaStatus status) { this.status = status; return this; }
        public SagaStateBuilder completedSteps(Set<SagaStep> completedSteps) { this.completedSteps = completedSteps; return this; }
        public SagaStateBuilder completedStepsFromCsv(String csv) {
            Set<SagaStep> steps = EnumSet.noneOf(SagaStep.class);
            if (csv != null && !csv.isEmpty()) {
                for (String s : csv.split(",")) {
                    steps.add(SagaStep.valueOf(s.trim()));
                }
            }
            this.completedSteps = steps;
            return this;
        }
        public SagaStateBuilder compensationAttempts(int compensationAttempts) { this.compensationAttempts = compensationAttempts; return this; }

        public SagaState build() {
            SagaState sagaState = SagaState.builder()
                    .orderId(orderId)
                    .orderNumber(orderNumber)
                    .build();
            ReflectionTestUtils.setField(sagaState, "id", id);
            ReflectionTestUtils.setField(sagaState, "currentStep", currentStep);
            ReflectionTestUtils.setField(sagaState, "status", status);
            ReflectionTestUtils.setField(sagaState, "completedSteps", completedSteps);
            ReflectionTestUtils.setField(sagaState, "compensationAttempts", compensationAttempts);
            return sagaState;
        }
    }
}
