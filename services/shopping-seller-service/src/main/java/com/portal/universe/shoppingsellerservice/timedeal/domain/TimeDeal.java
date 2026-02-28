package com.portal.universe.shoppingsellerservice.timedeal.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
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
public class TimeDeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeDealStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeDealProduct> products = new ArrayList<>();

    @Builder
    public TimeDeal(Long sellerId, String name, String description,
                    Instant startsAt, Instant endsAt) {
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.status = TimeDealStatus.SCHEDULED;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void addProduct(TimeDealProduct product) {
        this.products.add(product);
    }

    public void activate() {
        this.status = TimeDealStatus.ACTIVE;
    }

    public void end() {
        this.status = TimeDealStatus.ENDED;
    }

    public void cancel() {
        this.status = TimeDealStatus.CANCELLED;
    }

    public boolean isCancellable() {
        return this.status == TimeDealStatus.SCHEDULED || this.status == TimeDealStatus.ACTIVE;
    }
}
