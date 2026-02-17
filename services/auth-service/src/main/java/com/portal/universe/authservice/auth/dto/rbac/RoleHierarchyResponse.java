package com.portal.universe.authservice.auth.dto.rbac;

import java.util.List;
import java.util.Map;

public record RoleHierarchyResponse(
        Map<String, List<String>> graph
) {}
