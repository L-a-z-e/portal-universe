package com.portal.universe.authservice.auth.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_default_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_key", "membership_group"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleDefaultMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_key", nullable = false, length = 50)
    private String roleKey;

    @Column(name = "membership_group", nullable = false, length = 50)
    private String membershipGroup;

    @Column(name = "default_tier_key", nullable = false, length = 50)
    private String defaultTierKey;

    @Builder
    public RoleDefaultMembership(String roleKey, String membershipGroup, String defaultTierKey) {
        this.roleKey = roleKey;
        this.membershipGroup = membershipGroup;
        this.defaultTierKey = defaultTierKey;
    }
}
