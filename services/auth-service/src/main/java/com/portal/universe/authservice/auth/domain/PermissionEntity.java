package com.portal.universe.authservice.auth.domain;

import com.portal.universe.commonlibrary.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey;

    @Column(nullable = false, length = 50)
    private String service;

    @Column(nullable = false, length = 50)
    private String resource;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder
    public PermissionEntity(String permissionKey, String service, String resource,
                            String action, String description) {
        this.permissionKey = permissionKey;
        this.service = service;
        this.resource = resource;
        this.action = action;
        this.description = description;
        this.active = true;
    }
}
