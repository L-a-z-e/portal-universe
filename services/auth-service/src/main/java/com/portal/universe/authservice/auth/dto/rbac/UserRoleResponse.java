package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.UserRole;

import java.time.LocalDateTime;

public record UserRoleResponse(
        Long id,
        String roleKey,
        String displayName,
        String assignedBy,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt
) {
    public static UserRoleResponse from(UserRole userRole) {
        return new UserRoleResponse(
                userRole.getId(),
                userRole.getRole().getRoleKey(),
                userRole.getRole().getDisplayName(),
                userRole.getAssignedBy(),
                userRole.getAssignedAt(),
                userRole.getExpiresAt()
        );
    }
}
