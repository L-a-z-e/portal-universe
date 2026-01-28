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
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_key", nullable = false, unique = true, length = 50)
    private String roleKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 500)
    private String description;

    @Column(name = "service_scope", length = 50)
    private String serviceScope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_role_id")
    private RoleEntity parentRole;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public RoleEntity(String roleKey, String displayName, String description,
                      String serviceScope, RoleEntity parentRole, boolean system) {
        this.roleKey = roleKey;
        this.displayName = displayName;
        this.description = description;
        this.serviceScope = serviceScope;
        this.parentRole = parentRole;
        this.system = system;
        this.active = true;
    }

    public void deactivate() {
        if (this.system) {
            throw new IllegalStateException("System role cannot be deactivated");
        }
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void update(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
