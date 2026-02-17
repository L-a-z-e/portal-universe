package com.portal.universe.authservice.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_includes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"role_id", "included_role_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RoleInclude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "included_role_id", nullable = false)
    private RoleEntity includedRole;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public RoleInclude(RoleEntity role, RoleEntity includedRole) {
        this.role = role;
        this.includedRole = includedRole;
    }
}
