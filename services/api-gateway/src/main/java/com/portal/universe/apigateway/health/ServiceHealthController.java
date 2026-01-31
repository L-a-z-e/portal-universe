package com.portal.universe.apigateway.health;

import com.portal.universe.apigateway.health.dto.ServiceHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class ServiceHealthController {

    private final ServiceHealthAggregator healthAggregator;

    @GetMapping("/services")
    public Mono<ServiceHealthResponse> getServicesHealth() {
        return healthAggregator.aggregateHealth();
    }
}
