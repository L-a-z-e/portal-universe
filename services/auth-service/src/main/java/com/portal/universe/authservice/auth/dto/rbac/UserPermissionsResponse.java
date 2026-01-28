package com.portal.universe.authservice.auth.dto.rbac;

import java.util.List;
import java.util.Map;

public record UserPermissionsResponse(
        String userId,
        List<String> roles,
        List<String> permissions,
        Map<String, String> memberships
) {}
