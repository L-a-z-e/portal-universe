package com.portal.universe.shoppingservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_deals")
@Getter
@NoArgsConstructor
public class TimeDeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_time_deal_id", unique = true)
    private Long sourceTimeDealId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeDealStatus status = TimeDealStatus.SCHEDULED;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeDealProduct> products = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Builder
    public TimeDeal(Long sourceTimeDealId, Long sellerId, String name, String description,
                    Instant startsAt, Instant endsAt) {
        this.sourceTimeDealId = sourceTimeDealId;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = TimeDealStatus.SCHEDULED;
        this.createdAt = Instant.now();
    }

    public void activate() {
        this.status = TimeDealStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void end() {
        this.status = TimeDealStatus.ENDED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = TimeDealStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void updateFromSource(String name, String description, Instant startsAt,
                                  Instant endsAt, TimeDealStatus status) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void addProduct(TimeDealProduct product) {
        this.products.add(product);
        product.setTimeDeal(this);
    }

    public boolean isActive() {
        Instant now = Instant.now();
        return this.status == TimeDealStatus.ACTIVE
                && now.isAfter(this.startsAt)
                && now.isBefore(this.endsAt);
    }

    public boolean shouldStart() {
        return this.status == TimeDealStatus.SCHEDULED
                && Instant.now().isAfter(this.startsAt);
    }

    public boolean shouldEnd() {
        return this.status == TimeDealStatus.ACTIVE
                && Instant.now().isAfter(this.endsAt);
    }
}
