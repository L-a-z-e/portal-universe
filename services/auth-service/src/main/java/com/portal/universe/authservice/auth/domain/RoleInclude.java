package com.portal.universe.authservice.auth.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_includes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "included_role_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleInclude extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "included_role_id", nullable = false)
    private RoleEntity includedRole;

    public RoleInclude(RoleEntity role, RoleEntity includedRole) {
        this.role = role;
        this.includedRole = includedRole;
    }
}
