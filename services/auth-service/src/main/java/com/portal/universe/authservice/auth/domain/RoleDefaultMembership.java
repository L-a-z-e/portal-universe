package com.portal.universe.authservice.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_default_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_key", "membership_group"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RoleDefaultMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_key", nullable = false, length = 50)
    private String roleKey;

    @Column(name = "membership_group", nullable = false, length = 50)
    private String membershipGroup;

    @Column(name = "default_tier_key", nullable = false, length = 50)
    private String defaultTierKey;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RoleDefaultMembership(String roleKey, String membershipGroup, String defaultTierKey) {
        this.roleKey = roleKey;
        this.membershipGroup = membershipGroup;
        this.defaultTierKey = defaultTierKey;
    }
}
