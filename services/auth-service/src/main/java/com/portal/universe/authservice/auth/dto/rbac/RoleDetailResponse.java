package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.PermissionEntity;
import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.RoleInclude;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record RoleDetailResponse(
        Long id,
        String roleKey,
        String displayName,
        String description,
        String serviceScope,
        String membershipGroup,
        List<String> includedRoleKeys,
        List<String> effectiveRoleKeys,
        boolean system,
        boolean active,
        String createdAt,
        String updatedAt,
        List<PermissionResponse> permissions
) {
    public static RoleDetailResponse from(RoleEntity entity, List<RoleInclude> includes,
                                          List<String> effectiveRoleKeys,
                                          List<PermissionEntity> permissions) {
        List<String> includeKeys = includes.stream()
                .map(ri -> ri.getIncludedRole().getRoleKey())
                .toList();
        return new RoleDetailResponse(
                entity.getId(),
                entity.getRoleKey(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getServiceScope(),
                entity.getMembershipGroup(),
                includeKeys,
                effectiveRoleKeys,
                entity.isSystem(),
                entity.isActive(),
                entity.getCreatedAt() != null
                        ? entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                entity.getUpdatedAt() != null
                        ? entity.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                permissions.stream().map(PermissionResponse::from).toList()
        );
    }
}
