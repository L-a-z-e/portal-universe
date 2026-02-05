package com.portal.universe.shoppingservice.timedeal.domain;

import com.portal.universe.shoppingservice.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDealTest {

    private TimeDeal createScheduledTimeDeal(LocalDateTime startsAt, LocalDateTime endsAt) {
        return TimeDeal.builder()
                .name("타임딜 테스트")
                .description("테스트 설명")
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
    }

    @Nested
    @DisplayName("builder")
    class BuilderTest {

        @Test
        @DisplayName("should create time deal with SCHEDULED status")
        void should_create_with_scheduled_status() {
            LocalDateTime startsAt = LocalDateTime.now().plusHours(1);
            LocalDateTime endsAt = LocalDateTime.now().plusHours(3);

            TimeDeal timeDeal = createScheduledTimeDeal(startsAt, endsAt);

            assertThat(timeDeal.getName()).isEqualTo("타임딜 테스트");
            assertThat(timeDeal.getDescription()).isEqualTo("테스트 설명");
            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.SCHEDULED);
            assertThat(timeDeal.getStartsAt()).isEqualTo(startsAt);
            assertThat(timeDeal.getEndsAt()).isEqualTo(endsAt);
            assertThat(timeDeal.getProducts()).isEmpty();
            assertThat(timeDeal.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("activate")
    class ActivateTest {

        @Test
        @DisplayName("should change status to ACTIVE")
        void should_change_status_to_active() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3));

            timeDeal.activate();

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ACTIVE);
            assertThat(timeDeal.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("end")
    class EndTest {

        @Test
        @DisplayName("should change status to ENDED")
        void should_change_status_to_ended() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3));

            timeDeal.end();

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.ENDED);
            assertThat(timeDeal.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("should change status to CANCELLED")
        void should_change_status_to_cancelled() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3));

            timeDeal.cancel();

            assertThat(timeDeal.getStatus()).isEqualTo(TimeDealStatus.CANCELLED);
            assertThat(timeDeal.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("addProduct")
    class AddProductTest {

        @Test
        @DisplayName("should add product and set bidirectional relationship")
        void should_add_product() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3));

            Product product = Product.builder()
                    .name("테스트 상품")
                    .price(new BigDecimal("10000"))
                    .stock(100)
                    .build();

            TimeDealProduct timeDealProduct = TimeDealProduct.builder()
                    .product(product)
                    .dealPrice(new BigDecimal("5000"))
                    .dealQuantity(50)
                    .maxPerUser(3)
                    .build();

            timeDeal.addProduct(timeDealProduct);

            assertThat(timeDeal.getProducts()).hasSize(1);
            assertThat(timeDeal.getProducts().get(0).getTimeDeal()).isEqualTo(timeDeal);
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTest {

        @Test
        @DisplayName("should return true when status is ACTIVE and within time range")
        void should_return_true_when_active_and_in_range() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusHours(3));
            timeDeal.activate();

            assertThat(timeDeal.isActive()).isTrue();
        }

        @Test
        @DisplayName("should return false when status is SCHEDULED")
        void should_return_false_when_scheduled() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusHours(3));

            assertThat(timeDeal.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("shouldStart")
    class ShouldStartTest {

        @Test
        @DisplayName("should return true when scheduled and start time has passed")
        void should_return_true_when_should_start() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now().plusHours(3));

            assertThat(timeDeal.shouldStart()).isTrue();
        }

        @Test
        @DisplayName("should return false when start time has not passed")
        void should_return_false_when_not_yet() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3));

            assertThat(timeDeal.shouldStart()).isFalse();
        }
    }

    @Nested
    @DisplayName("shouldEnd")
    class ShouldEndTest {

        @Test
        @DisplayName("should return true when active and end time has passed")
        void should_return_true_when_should_end() {
            TimeDeal timeDeal = createScheduledTimeDeal(
                    LocalDateTime.now().minusHours(3),
                    LocalDateTime.now().minusMinutes(1));
            timeDeal.activate();

            assertThat(timeDeal.shouldEnd()).isTrue();
        }
    }
}
