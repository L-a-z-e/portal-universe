package com.portal.universe.apigateway.health;

import com.portal.universe.apigateway.health.config.HealthCheckProperties;
import com.portal.universe.apigateway.health.dto.PodInfo;
import com.portal.universe.apigateway.health.dto.ServiceHealthInfo;
import com.portal.universe.apigateway.health.dto.ServiceHealthResponse;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ServiceHealthAggregator {

    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(3);

    private final HealthCheckProperties properties;
    private final WebClient webClient;
    private final KubernetesClient kubernetesClient;
    private final HealthEndpoint healthEndpoint;

    @Value("${spring.application.name:api-gateway}")
    private String applicationName;

    @Value("${KUBERNETES_NAMESPACE:portal-universe}")
    private String namespace;

    public ServiceHealthAggregator(
            HealthCheckProperties properties,
            @Autowired(required = false) KubernetesClient kubernetesClient,
            HealthEndpoint healthEndpoint
    ) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.kubernetesClient = kubernetesClient;
        this.healthEndpoint = healthEndpoint;
    }

    public Mono<ServiceHealthResponse> aggregateHealth() {
        List<HealthCheckProperties.ServiceConfig> configs = properties.getServices();

        return Flux.fromIterable(configs)
                .flatMap(this::checkService)
                .collectList()
                .map(ServiceHealthResponse::of);
    }

    private Mono<ServiceHealthInfo> checkService(HealthCheckProperties.ServiceConfig config) {
        // gateway 자체 health는 HealthEndpoint를 직접 호출 (self-call timeout 방지)
        if (applicationName.equals(config.getName())) {
            return checkSelf(config);
        }

        String healthUrl = config.getUrl() + config.getHealthPath();
        long startTime = System.currentTimeMillis();
        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(HEALTH_CHECK_TIMEOUT)
                .map(body -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    String status = resolveStatus(body);
                    return ServiceHealthInfo.of(config.getName(), config.getDisplayName(), status, responseTime);
                })
                .onErrorResume(error -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    log.warn("Health check failed for {} ({}): {}", config.getName(), healthUrl, error.getMessage());
                    return Mono.just(ServiceHealthInfo.of(config.getName(), config.getDisplayName(), "down", responseTime));
                })
                .flatMap(info -> enrichWithKubernetesInfoAsync(info, config));
    }

    private Mono<ServiceHealthInfo> checkSelf(HealthCheckProperties.ServiceConfig config) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                var health = healthEndpoint.health();
                long responseTime = System.currentTimeMillis() - startTime;
                Status status = health.getStatus();
                String s = Status.UP.equals(status) ? "up"
                        : Status.DOWN.equals(status) ? "down" : "degraded";
                log.debug("Self health check: status={}, resolved={}", status, s);
                return ServiceHealthInfo.of(config.getName(), config.getDisplayName(), s, responseTime);
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                log.warn("Self health check failed: {}", e.getMessage());
                return ServiceHealthInfo.of(config.getName(), config.getDisplayName(), "down", responseTime);
            }
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(info -> enrichWithKubernetesInfoAsync(info, config));
    }

    @SuppressWarnings("unchecked")
    private String resolveStatus(Map<String, Object> body) {
        // Spring Boot Actuator: {"status": "UP", ...}
        Object status = body.get("status");
        if (status instanceof String s) {
            if (s.equalsIgnoreCase("UP")) return "up";
            if (s.equalsIgnoreCase("DOWN")) return "down";
            return "degraded";
        }

        // Custom format (prism-service): {"success": true, "data": {"status": "ok"}}
        Object success = body.get("success");
        if (Boolean.TRUE.equals(success)) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object dataStatus = dataMap.get("status");
                if (dataStatus instanceof String ds && ds.equalsIgnoreCase("ok")) {
                    return "up";
                }
            }
            return "up";
        }

        return "unknown";
    }

    private Mono<ServiceHealthInfo> enrichWithKubernetesInfoAsync(ServiceHealthInfo info, HealthCheckProperties.ServiceConfig config) {
        if (kubernetesClient == null || config.getK8sDeploymentName() == null) {
            return Mono.just(info);
        }
        return Mono.fromCallable(() -> enrichWithKubernetesInfo(info, config))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(2))
                .onErrorReturn(info);
    }

    private ServiceHealthInfo enrichWithKubernetesInfo(ServiceHealthInfo info, HealthCheckProperties.ServiceConfig config) {
        if (kubernetesClient == null || config.getK8sDeploymentName() == null) {
            return info;
        }

        try {
            Deployment deployment = kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(config.getK8sDeploymentName())
                    .get();

            if (deployment == null || deployment.getStatus() == null) {
                return info;
            }

            int replicas = deployment.getStatus().getReplicas() != null
                    ? deployment.getStatus().getReplicas() : 0;
            int readyReplicas = deployment.getStatus().getReadyReplicas() != null
                    ? deployment.getStatus().getReadyReplicas() : 0;

            List<Pod> pods = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withLabel("app", config.getK8sDeploymentName())
                    .list()
                    .getItems();

            List<PodInfo> podInfos = pods.stream()
                    .map(this::toPodInfo)
                    .toList();

            return info.withKubernetesInfo(replicas, readyReplicas, podInfos);
        } catch (Exception e) {
            log.warn("Failed to get K8s info for {}: {}", config.getK8sDeploymentName(), e.getMessage());
            return info;
        }
    }

    private PodInfo toPodInfo(Pod pod) {
        String name = pod.getMetadata().getName();
        String phase = pod.getStatus().getPhase();

        List<ContainerStatus> containerStatuses = pod.getStatus().getContainerStatuses();
        boolean ready = containerStatuses != null && !containerStatuses.isEmpty()
                && containerStatuses.stream().allMatch(ContainerStatus::getReady);
        int restarts = containerStatuses != null
                ? containerStatuses.stream().mapToInt(ContainerStatus::getRestartCount).sum()
                : 0;

        return new PodInfo(name, phase, ready, restarts);
    }
}
