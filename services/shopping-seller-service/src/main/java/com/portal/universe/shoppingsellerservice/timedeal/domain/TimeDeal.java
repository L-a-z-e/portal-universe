package com.portal.universe.shoppingsellerservice.timedeal.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_deals")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class TimeDeal {

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
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @OneToMany(mappedBy = "timeDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeDealProduct> products = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public TimeDeal(Long sellerId, String name, String description,
                    LocalDateTime startsAt, LocalDateTime endsAt) {
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

    public void cancel() {
        this.status = TimeDealStatus.CANCELLED;
    }

    public boolean isCancellable() {
        return this.status == TimeDealStatus.SCHEDULED || this.status == TimeDealStatus.ACTIVE;
    }
}
