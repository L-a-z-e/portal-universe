package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.PermissionEntity;

public record PermissionResponse(
        Long id,
        String permissionKey,
        String service,
        String resource,
        String action,
        String description,
        boolean active
) {
    public static PermissionResponse from(PermissionEntity entity) {
        return new PermissionResponse(
                entity.getId(),
                entity.getPermissionKey(),
                entity.getService(),
                entity.getResource(),
                entity.getAction(),
                entity.getDescription(),
                entity.isActive()
        );
    }
}
