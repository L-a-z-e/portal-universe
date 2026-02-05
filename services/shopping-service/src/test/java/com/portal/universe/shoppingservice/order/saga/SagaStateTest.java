package com.portal.universe.shoppingservice.order.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStateTest {

    private SagaState createTestSagaState() {
        return SagaState.builder()
                .orderId(1L)
                .orderNumber("ORD-001")
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create saga state with default values")
        void should_create_with_defaults() {
            SagaState saga = createTestSagaState();

            assertThat(saga.getOrderId()).isEqualTo(1L);
            assertThat(saga.getOrderNumber()).isEqualTo("ORD-001");
            assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
            assertThat(saga.getCompletedSteps()).isEmpty();
            assertThat(saga.getCompensationAttempts()).isEqualTo(0);
            assertThat(saga.getLastErrorMessage()).isNull();
            assertThat(saga.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("sagaId generation")
    class SagaIdTest {

        @Test
        @DisplayName("should generate saga id starting with SAGA-")
        void should_generate_saga_id() {
            SagaState saga = createTestSagaState();

            assertThat(saga.getSagaId()).isNotNull();
            assertThat(saga.getSagaId()).startsWith("SAGA-");
            assertThat(saga.getSagaId()).hasSize(13); // "SAGA-" + 8 chars
        }

        @Test
        @DisplayName("should generate unique saga ids")
        void should_generate_unique_ids() {
            SagaState saga1 = createTestSagaState();
            SagaState saga2 = createTestSagaState();

            assertThat(saga1.getSagaId()).isNotEqualTo(saga2.getSagaId());
        }
    }

    @Nested
    @DisplayName("proceedToNextStep")
    class ProceedToNextStepTest {

        @Test
        @DisplayName("should move from RESERVE_INVENTORY to PROCESS_PAYMENT")
        void should_proceed_to_next_step() {
            SagaState saga = createTestSagaState();

            saga.proceedToNextStep();

            assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.PROCESS_PAYMENT);
            assertThat(saga.getCompletedSteps()).contains("RESERVE_INVENTORY");
        }

        @Test
        @DisplayName("should accumulate completed steps across multiple transitions")
        void should_accumulate_completed_steps() {
            SagaState saga = createTestSagaState();

            saga.proceedToNextStep(); // RESERVE_INVENTORY -> PROCESS_PAYMENT
            saga.proceedToNextStep(); // PROCESS_PAYMENT -> DEDUCT_INVENTORY

            assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.DEDUCT_INVENTORY);
            assertThat(saga.getCompletedSteps()).contains("RESERVE_INVENTORY");
            assertThat(saga.getCompletedSteps()).contains("PROCESS_PAYMENT");
        }
    }

    @Nested
    @DisplayName("complete")
    class CompleteTest {

        @Test
        @DisplayName("should mark saga as completed with timestamp")
        void should_complete_saga() {
            SagaState saga = createTestSagaState();
            saga.proceedToNextStep();
            saga.proceedToNextStep();
            saga.proceedToNextStep();
            saga.proceedToNextStep();

            saga.complete();

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
            assertThat(saga.getCompletedAt()).isNotNull();
            assertThat(saga.getCompletedSteps()).contains("CONFIRM_ORDER");
        }
    }

    @Nested
    @DisplayName("startCompensation")
    class StartCompensationTest {

        @Test
        @DisplayName("should set status to COMPENSATING with error message")
        void should_start_compensation() {
            SagaState saga = createTestSagaState();

            saga.startCompensation("재고 부족");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATING);
            assertThat(saga.getLastErrorMessage()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("should overwrite previous error message")
        void should_overwrite_error_message() {
            SagaState saga = createTestSagaState();
            saga.startCompensation("첫 번째 에러");

            saga.startCompensation("두 번째 에러");

            assertThat(saga.getLastErrorMessage()).isEqualTo("두 번째 에러");
        }
    }

    @Nested
    @DisplayName("rollbackToPreviousStep")
    class RollbackTest {

        @Test
        @DisplayName("should move to previous step")
        void should_rollback_to_previous_step() {
            SagaState saga = createTestSagaState();
            saga.proceedToNextStep(); // now at PROCESS_PAYMENT

            saga.rollbackToPreviousStep();

            assertThat(saga.getCurrentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
        }
    }

    @Nested
    @DisplayName("markAsFailed")
    class MarkAsFailedTest {

        @Test
        @DisplayName("should mark saga as failed with error message and timestamp")
        void should_mark_as_failed() {
            SagaState saga = createTestSagaState();

            saga.markAsFailed("결제 실패");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.FAILED);
            assertThat(saga.getLastErrorMessage()).isEqualTo("결제 실패");
            assertThat(saga.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("markAsCompensationFailed")
    class MarkAsCompensationFailedTest {

        @Test
        @DisplayName("should mark saga as compensation failed")
        void should_mark_as_compensation_failed() {
            SagaState saga = createTestSagaState();

            saga.markAsCompensationFailed("보상 트랜잭션 실패");

            assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_FAILED);
            assertThat(saga.getLastErrorMessage()).isEqualTo("보상 트랜잭션 실패");
            assertThat(saga.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("incrementCompensationAttempts")
    class IncrementCompensationAttemptsTest {

        @Test
        @DisplayName("should increment compensation attempts counter")
        void should_increment_attempts() {
            SagaState saga = createTestSagaState();

            saga.incrementCompensationAttempts();
            saga.incrementCompensationAttempts();

            assertThat(saga.getCompensationAttempts()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("isStepCompleted")
    class IsStepCompletedTest {

        @Test
        @DisplayName("should return true for completed step")
        void should_return_true_for_completed_step() {
            SagaState saga = createTestSagaState();
            saga.proceedToNextStep(); // completes RESERVE_INVENTORY

            assertThat(saga.isStepCompleted(SagaStep.RESERVE_INVENTORY)).isTrue();
        }

        @Test
        @DisplayName("should return false for uncompleted step")
        void should_return_false_for_uncompleted_step() {
            SagaState saga = createTestSagaState();

            assertThat(saga.isStepCompleted(SagaStep.PROCESS_PAYMENT)).isFalse();
        }
    }
}
