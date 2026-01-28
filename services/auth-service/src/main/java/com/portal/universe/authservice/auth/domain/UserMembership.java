package com.portal.universe.authservice.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "service_name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public UserMembership(String userId, String serviceName, MembershipTier tier,
                          LocalDateTime expiresAt, boolean autoRenew) {
        this.userId = userId;
        this.serviceName = serviceName;
        this.tier = tier;
        this.status = MembershipStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
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

    public void renew(LocalDateTime newExpiresAt) {
        this.status = MembershipStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = newExpiresAt;
    }

    public boolean isActive() {
        return this.status == MembershipStatus.ACTIVE;
    }
}
