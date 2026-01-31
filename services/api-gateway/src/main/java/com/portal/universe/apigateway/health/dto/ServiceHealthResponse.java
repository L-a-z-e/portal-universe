package com.portal.universe.apigateway.health.dto;

import java.time.Instant;
import java.util.List;

public record ServiceHealthResponse(
        String overallStatus,
        Instant timestamp,
        List<ServiceHealthInfo> services
) {

    public static ServiceHealthResponse of(List<ServiceHealthInfo> services) {
        String overall = resolveOverallStatus(services);
        return new ServiceHealthResponse(overall, Instant.now(), services);
    }

    private static String resolveOverallStatus(List<ServiceHealthInfo> services) {
        if (services.isEmpty()) return "unknown";

        boolean allUp = services.stream().allMatch(s -> "up".equals(s.status()));
        if (allUp) return "up";

        boolean allDown = services.stream().allMatch(s -> "down".equals(s.status()));
        if (allDown) return "down";

        return "degraded";
    }
}
