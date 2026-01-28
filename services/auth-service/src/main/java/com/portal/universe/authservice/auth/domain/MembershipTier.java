package com.portal.universe.authservice.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_tiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"service_name", "tier_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @Column(name = "tier_key", nullable = false, length = 50)
    private String tierKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "price_monthly", precision = 10, scale = 2)
    private BigDecimal priceMonthly;

    @Column(name = "price_yearly", precision = 10, scale = 2)
    private BigDecimal priceYearly;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MembershipTier(String serviceName, String tierKey, String displayName,
                          BigDecimal priceMonthly, BigDecimal priceYearly, int sortOrder) {
        this.serviceName = serviceName;
        this.tierKey = tierKey;
        this.displayName = displayName;
        this.priceMonthly = priceMonthly;
        this.priceYearly = priceYearly;
        this.sortOrder = sortOrder;
        this.active = true;
    }
}
