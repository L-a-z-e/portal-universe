package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.RoleInclude;

import java.util.List;

public record RoleResponse(
        Long id,
        String roleKey,
        String displayName,
        String description,
        String serviceScope,
        String membershipGroup,
        List<String> includedRoleKeys,
        boolean system,
        boolean active
) {
    public static RoleResponse from(RoleEntity entity, List<RoleInclude> includes) {
        List<String> includeKeys = includes.stream()
                .map(ri -> ri.getIncludedRole().getRoleKey())
                .toList();
        return new RoleResponse(
                entity.getId(),
                entity.getRoleKey(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getServiceScope(),
                entity.getMembershipGroup(),
                includeKeys,
                entity.isSystem(),
                entity.isActive()
        );
    }
}
