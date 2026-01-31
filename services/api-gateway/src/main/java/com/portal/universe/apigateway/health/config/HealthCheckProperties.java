package com.portal.universe.apigateway.health.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "health-check")
public class HealthCheckProperties {

    private List<ServiceConfig> services = new ArrayList<>();

    @Data
    public static class ServiceConfig {
        private String name;
        private String displayName;
        private String url;
        private String healthPath = "/actuator/health";
        private String k8sDeploymentName;
    }
}
