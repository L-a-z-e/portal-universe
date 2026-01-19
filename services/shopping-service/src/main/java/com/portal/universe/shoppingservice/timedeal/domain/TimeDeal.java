package com.portal.universe.shoppingservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeDealStatus status = TimeDealStatus.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeDealProduct> products = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Builder
    public TimeDeal(String name, String description, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = TimeDealStatus.SCHEDULED;
        this.createdAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = TimeDealStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void end() {
        this.status = TimeDealStatus.ENDED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TimeDealStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void addProduct(TimeDealProduct product) {
        this.products.add(product);
        product.setTimeDeal(this);
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return this.status == TimeDealStatus.ACTIVE
                && now.isAfter(this.startsAt)
                && now.isBefore(this.endsAt);
    }

    public boolean shouldStart() {
        return this.status == TimeDealStatus.SCHEDULED
                && LocalDateTime.now().isAfter(this.startsAt);
    }

    public boolean shouldEnd() {
        return this.status == TimeDealStatus.ACTIVE
                && LocalDateTime.now().isAfter(this.endsAt);
    }
}
