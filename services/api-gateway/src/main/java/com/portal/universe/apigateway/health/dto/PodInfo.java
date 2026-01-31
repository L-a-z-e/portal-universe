package com.portal.universe.apigateway.health.dto;

public record PodInfo(
        String name,
        String phase,
        boolean ready,
        int restarts
) {}
