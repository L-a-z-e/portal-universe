package com.portal.universe.authservice.auth.dto.rbac;

import java.util.List;

public record ResolvedRoleResponse(
        String roleKey,
        List<String> effectiveRoles,
        List<String> effectivePermissions
) {}
