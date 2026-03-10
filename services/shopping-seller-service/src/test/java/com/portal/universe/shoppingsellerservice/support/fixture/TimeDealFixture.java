package com.portal.universe.shoppingsellerservice.support.fixture;

import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingsellerservice.timedeal.domain.TimeDealStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimeDealFixture {

    private TimeDealFixture() {}

    public static TimeDeal create() {
        return builder().build();
    }

    public static TimeDealBuilder builder() {
        return new TimeDealBuilder();
    }

    public static class TimeDealBuilder {
        private Long id;
        private Long sellerId = 1L;
        private String name = "Test Time Deal";
        private String description = "Test time deal description";
        private TimeDealStatus status;
        private Instant startsAt = Instant.now().plus(1, ChronoUnit.HOURS);
        private Instant endsAt = Instant.now().plus(2, ChronoUnit.HOURS);

        public TimeDealBuilder id(Long id) { this.id = id; return this; }
        public TimeDealBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public TimeDealBuilder name(String name) { this.name = name; return this; }
        public TimeDealBuilder description(String description) { this.description = description; return this; }
        public TimeDealBuilder status(TimeDealStatus status) { this.status = status; return this; }
        public TimeDealBuilder startsAt(Instant startsAt) { this.startsAt = startsAt; return this; }
        public TimeDealBuilder endsAt(Instant endsAt) { this.endsAt = endsAt; return this; }

        public TimeDeal build() {
            TimeDeal timeDeal = TimeDeal.builder()
                    .sellerId(sellerId)
                    .name(name)
                    .description(description)
                    .startsAt(startsAt)
                    .endsAt(endsAt)
                    .build();
            if (id != null) {
                ReflectionTestUtils.setField(timeDeal, "id", id);
            }
            if (status != null && status != TimeDealStatus.SCHEDULED) {
                ReflectionTestUtils.setField(timeDeal, "status", status);
            }
            return timeDeal;
        }
    }

    public static TimeDealProduct createProduct(TimeDeal timeDeal, Long productId) {
        return createProduct(timeDeal, productId, new BigDecimal("5000"), 50, 5);
    }

    public static TimeDealProduct createProduct(TimeDeal timeDeal, Long productId,
                                                 BigDecimal dealPrice, Integer dealQuantity, Integer maxPerUser) {
        TimeDealProduct product = TimeDealProduct.builder()
                .timeDeal(timeDeal)
                .productId(productId)
                .dealPrice(dealPrice)
                .dealQuantity(dealQuantity)
                .maxPerUser(maxPerUser)
                .build();
        return product;
    }
}
