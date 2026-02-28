package com.portal.universe.authservice.auth.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "membership_group"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "membership_group", nullable = false, length = 50)
    private String membershipGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Builder
    public UserMembership(String userId, String membershipGroup, MembershipTier tier,
                          Instant expiresAt, boolean autoRenew) {
        this.userId = userId;
        this.membershipGroup = membershipGroup;
        this.tier = tier;
        this.status = MembershipStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    public void changeTier(MembershipTier newTier) {
        this.tier = newTier;
    }

    public void cancel() {
        this.status = MembershipStatus.CANCELLED;
        this.autoRenew = false;
    }

    public void expire() {
        this.status = MembershipStatus.EXPIRED;
    }

    public void renew(Instant newExpiresAt) {
        this.status = MembershipStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.expiresAt = newExpiresAt;
    }

    public boolean isActive() {
        return this.status == MembershipStatus.ACTIVE;
    }
}
