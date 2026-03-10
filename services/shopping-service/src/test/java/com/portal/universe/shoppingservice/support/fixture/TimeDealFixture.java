package com.portal.universe.shoppingservice.support.fixture;

import com.portal.universe.shoppingservice.timedeal.domain.TimeDeal;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealProduct;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealPurchase;
import com.portal.universe.shoppingservice.timedeal.domain.TimeDealStatus;
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
        private Long id = 1L;
        private Long sellerId = 1L;
        private String name = "Test Deal";
        private TimeDealStatus status = TimeDealStatus.ACTIVE;
        private Instant startsAt = Instant.now().minus(1, ChronoUnit.HOURS);
        private Instant endsAt = Instant.now().plus(5, ChronoUnit.HOURS);

        public TimeDealBuilder id(Long id) { this.id = id; return this; }
        public TimeDealBuilder sellerId(Long sellerId) { this.sellerId = sellerId; return this; }
        public TimeDealBuilder name(String name) { this.name = name; return this; }
        public TimeDealBuilder status(TimeDealStatus status) { this.status = status; return this; }
        public TimeDealBuilder startsAt(Instant startsAt) { this.startsAt = startsAt; return this; }
        public TimeDealBuilder endsAt(Instant endsAt) { this.endsAt = endsAt; return this; }

        public TimeDeal build() {
            TimeDeal deal = TimeDeal.builder()
                    .sellerId(sellerId)
                    .name(name)
                    .startsAt(startsAt)
                    .endsAt(endsAt)
                    .build();
            ReflectionTestUtils.setField(deal, "id", id);
            if (status != TimeDealStatus.SCHEDULED) {
                ReflectionTestUtils.setField(deal, "status", status);
            }
            return deal;
        }
    }

    public static TimeDealProduct createProduct(TimeDeal deal, Long productId) {
        return productBuilder().timeDeal(deal).productId(productId).build();
    }

    public static TimeDealProduct createProduct(TimeDeal deal, Long productId, int soldQuantity) {
        return productBuilder().timeDeal(deal).productId(productId).soldQuantity(soldQuantity).build();
    }

    public static ProductBuilder productBuilder() {
        return new ProductBuilder();
    }

    public static class ProductBuilder {
        private Long id = 1L;
        private TimeDeal timeDeal;
        private Long productId = 1L;
        private BigDecimal dealPrice = BigDecimal.valueOf(5000);
        private int dealQuantity = 50;
        private int maxPerUser = 2;
        private int soldQuantity = 0;

        public ProductBuilder id(Long id) { this.id = id; return this; }
        public ProductBuilder timeDeal(TimeDeal timeDeal) { this.timeDeal = timeDeal; return this; }
        public ProductBuilder productId(Long productId) { this.productId = productId; return this; }
        public ProductBuilder dealPrice(BigDecimal dealPrice) { this.dealPrice = dealPrice; return this; }
        public ProductBuilder dealQuantity(int dealQuantity) { this.dealQuantity = dealQuantity; return this; }
        public ProductBuilder maxPerUser(int maxPerUser) { this.maxPerUser = maxPerUser; return this; }
        public ProductBuilder soldQuantity(int soldQuantity) { this.soldQuantity = soldQuantity; return this; }

        public TimeDealProduct build() {
            TimeDealProduct tdp = TimeDealProduct.builder()
                    .productId(productId)
                    .dealPrice(dealPrice)
                    .dealQuantity(dealQuantity)
                    .maxPerUser(maxPerUser)
                    .build();
            tdp.setTimeDeal(timeDeal);
            ReflectionTestUtils.setField(tdp, "id", id);
            if (soldQuantity > 0) {
                ReflectionTestUtils.setField(tdp, "soldQuantity", soldQuantity);
            }
            return tdp;
        }
    }

    public static TimeDealPurchase createPurchase(Long id, String userId, TimeDealProduct product, int quantity) {
        return purchaseBuilder().id(id).userId(userId).timeDealProduct(product).quantity(quantity).build();
    }

    public static PurchaseBuilder purchaseBuilder() {
        return new PurchaseBuilder();
    }

    public static class PurchaseBuilder {
        private Long id = 1L;
        private String userId = "test-user-001";
        private TimeDealProduct timeDealProduct;
        private int quantity = 1;
        private BigDecimal purchasePrice;

        public PurchaseBuilder id(Long id) { this.id = id; return this; }
        public PurchaseBuilder userId(String userId) { this.userId = userId; return this; }
        public PurchaseBuilder timeDealProduct(TimeDealProduct timeDealProduct) { this.timeDealProduct = timeDealProduct; return this; }
        public PurchaseBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public PurchaseBuilder purchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; return this; }

        public TimeDealPurchase build() {
            TimeDealPurchase purchase = TimeDealPurchase.builder()
                    .userId(userId)
                    .timeDealProduct(timeDealProduct)
                    .quantity(quantity)
                    .purchasePrice(purchasePrice != null ? purchasePrice : timeDealProduct.getDealPrice())
                    .build();
            ReflectionTestUtils.setField(purchase, "id", id);
            return purchase;
        }
    }
}
