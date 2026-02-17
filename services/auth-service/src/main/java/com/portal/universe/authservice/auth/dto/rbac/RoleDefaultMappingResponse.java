package com.portal.universe.authservice.auth.dto.rbac;

import com.portal.universe.authservice.auth.domain.RoleDefaultMembership;

public record RoleDefaultMappingResponse(
        Long id,
        String roleKey,
        String membershipGroup,
        String defaultTierKey
) {
    public static RoleDefaultMappingResponse from(RoleDefaultMembership entity) {
        return new RoleDefaultMappingResponse(
                entity.getId(),
                entity.getRoleKey(),
                entity.getMembershipGroup(),
                entity.getDefaultTierKey()
        );
    }
}
