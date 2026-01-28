package com.portal.universe.authservice.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "membership_tier_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tier_id", "permission_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipTierPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    public MembershipTierPermission(MembershipTier tier, PermissionEntity permission) {
        this.tier = tier;
        this.permission = permission;
    }
}
