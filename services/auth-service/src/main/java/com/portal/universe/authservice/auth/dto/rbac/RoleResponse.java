package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.RoleEntity;

public record RoleResponse(
        Long id,
        String roleKey,
        String displayName,
        String description,
        String serviceScope,
        String parentRoleKey,
        boolean system,
        boolean active
) {
    public static RoleResponse from(RoleEntity entity) {
        return new RoleResponse(
                entity.getId(),
                entity.getRoleKey(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getServiceScope(),
                entity.getParentRole() != null ? entity.getParentRole().getRoleKey() : null,
                entity.isSystem(),
                entity.isActive()
        );
    }
}
