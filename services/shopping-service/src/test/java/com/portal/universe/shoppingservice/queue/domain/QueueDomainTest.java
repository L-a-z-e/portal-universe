package com.portal.universe.shoppingservice.queue.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class QueueDomainTest {

    @Nested
    @DisplayName("WaitingQueue")
    class WaitingQueueTest {

        private WaitingQueue createTestQueue() {
            return WaitingQueue.builder()
                    .eventType("TIMEDEAL")
                    .eventId(1L)
                    .maxCapacity(100)
                    .entryBatchSize(10)
                    .entryIntervalSeconds(5)
                    .build();
        }

        @Nested
        @DisplayName("builder")
        class BuilderTest {

            @Test
            @DisplayName("should create waiting queue with default values")
            void should_create_with_defaults() {
                WaitingQueue queue = createTestQueue();

                assertThat(queue.getEventType()).isEqualTo("TIMEDEAL");
                assertThat(queue.getEventId()).isEqualTo(1L);
                assertThat(queue.getMaxCapacity()).isEqualTo(100);
                assertThat(queue.getEntryBatchSize()).isEqualTo(10);
                assertThat(queue.getEntryIntervalSeconds()).isEqualTo(5);
                assertThat(queue.getIsActive()).isFalse();
                assertThat(queue.getCreatedAt()).isNotNull();
                assertThat(queue.getActivatedAt()).isNull();
                assertThat(queue.getDeactivatedAt()).isNull();
            }
        }

        @Nested
        @DisplayName("activate")
        class ActivateTest {

            @Test
            @DisplayName("should set isActive to true and set activatedAt")
            void should_activate() {
                WaitingQueue queue = createTestQueue();

                queue.activate();

                assertThat(queue.getIsActive()).isTrue();
                assertThat(queue.getActivatedAt()).isNotNull();
            }
        }

        @Nested
        @DisplayName("deactivate")
        class DeactivateTest {

            @Test
            @DisplayName("should set isActive to false and set deactivatedAt")
            void should_deactivate() {
                WaitingQueue queue = createTestQueue();
                queue.activate();

                queue.deactivate();

                assertThat(queue.getIsActive()).isFalse();
                assertThat(queue.getDeactivatedAt()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("QueueEntry")
    class QueueEntryTest {

        private WaitingQueue createTestQueue() {
            return WaitingQueue.builder()
                    .eventType("TIMEDEAL")
                    .eventId(1L)
                    .maxCapacity(100)
                    .entryBatchSize(10)
                    .entryIntervalSeconds(5)
                    .build();
        }

        private QueueEntry createTestEntry() {
            return QueueEntry.builder()
                    .queue(createTestQueue())
                    .userId("user-001")
                    .build();
        }

        @Nested
        @DisplayName("builder")
        class BuilderTest {

            @Test
            @DisplayName("should create queue entry with WAITING status")
            void should_create_with_waiting_status() {
                QueueEntry entry = createTestEntry();

                assertThat(entry.getQueue()).isNotNull();
                assertThat(entry.getUserId()).isEqualTo("user-001");
                assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);
                assertThat(entry.getJoinedAt()).isNotNull();
                assertThat(entry.getEnteredAt()).isNull();
                assertThat(entry.getExpiredAt()).isNull();
                assertThat(entry.getLeftAt()).isNull();
            }
        }

        @Nested
        @DisplayName("entryToken")
        class EntryTokenTest {

            @Test
            @DisplayName("should generate unique entry token (UUID format)")
            void should_generate_unique_token() {
                QueueEntry entry1 = createTestEntry();
                QueueEntry entry2 = createTestEntry();

                assertThat(entry1.getEntryToken()).isNotNull();
                assertThat(entry1.getEntryToken()).hasSize(36); // UUID format
                assertThat(entry1.getEntryToken()).isNotEqualTo(entry2.getEntryToken());
            }
        }

        @Nested
        @DisplayName("enter")
        class EnterTest {

            @Test
            @DisplayName("should change status to ENTERED and set enteredAt")
            void should_enter() {
                QueueEntry entry = createTestEntry();

                entry.enter();

                assertThat(entry.getStatus()).isEqualTo(QueueStatus.ENTERED);
                assertThat(entry.getEnteredAt()).isNotNull();
            }
        }

        @Nested
        @DisplayName("expire")
        class ExpireTest {

            @Test
            @DisplayName("should change status to EXPIRED and set expiredAt")
            void should_expire() {
                QueueEntry entry = createTestEntry();

                entry.expire();

                assertThat(entry.getStatus()).isEqualTo(QueueStatus.EXPIRED);
                assertThat(entry.getExpiredAt()).isNotNull();
            }
        }

        @Nested
        @DisplayName("leave")
        class LeaveTest {

            @Test
            @DisplayName("should change status to LEFT and set leftAt")
            void should_leave() {
                QueueEntry entry = createTestEntry();

                entry.leave();

                assertThat(entry.getStatus()).isEqualTo(QueueStatus.LEFT);
                assertThat(entry.getLeftAt()).isNotNull();
            }
        }

        @Nested
        @DisplayName("isWaiting")
        class IsWaitingTest {

            @Test
            @DisplayName("should return true when status is WAITING")
            void should_return_true_when_waiting() {
                QueueEntry entry = createTestEntry();

                assertThat(entry.isWaiting()).isTrue();
            }

            @Test
            @DisplayName("should return false when status is ENTERED")
            void should_return_false_when_entered() {
                QueueEntry entry = createTestEntry();
                entry.enter();

                assertThat(entry.isWaiting()).isFalse();
            }
        }

        @Nested
        @DisplayName("isEntered")
        class IsEnteredTest {

            @Test
            @DisplayName("should return true when status is ENTERED")
            void should_return_true_when_entered() {
                QueueEntry entry = createTestEntry();
                entry.enter();

                assertThat(entry.isEntered()).isTrue();
            }

            @Test
            @DisplayName("should return false when status is WAITING")
            void should_return_false_when_waiting() {
                QueueEntry entry = createTestEntry();

                assertThat(entry.isEntered()).isFalse();
            }
        }
    }
}
