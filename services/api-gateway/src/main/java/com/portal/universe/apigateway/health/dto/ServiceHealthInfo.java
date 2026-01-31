package com.portal.universe.apigateway.health.dto;

import java.util.List;

public record ServiceHealthInfo(
        String name,
        String displayName,
        String status,
        Long responseTime,
        Integer replicas,
        Integer readyReplicas,
        List<PodInfo> pods
) {

    public static ServiceHealthInfo of(String name, String displayName, String status, long responseTime) {
        return new ServiceHealthInfo(name, displayName, status, responseTime, null, null, null);
    }

    public ServiceHealthInfo withKubernetesInfo(int replicas, int readyReplicas, List<PodInfo> pods) {
        return new ServiceHealthInfo(this.name, this.displayName, this.status, this.responseTime,
                replicas, readyReplicas, pods);
    }
}
