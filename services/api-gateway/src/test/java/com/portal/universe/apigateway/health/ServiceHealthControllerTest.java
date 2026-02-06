package com.portal.universe.apigateway.health;

import com.portal.universe.apigateway.health.dto.ServiceHealthInfo;
import com.portal.universe.apigateway.health.dto.ServiceHealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceHealthController Test")
class ServiceHealthControllerTest {

    @Mock
    private ServiceHealthAggregator healthAggregator;

    @InjectMocks
    private ServiceHealthController controller;

    @Test
    @DisplayName("ServiceHealthAggregator의 결과를 반환한다")
    void should_returnHealthResponse_when_called() {
        var expected = ServiceHealthResponse.of(List.of(
                ServiceHealthInfo.of("auth", "Auth Service", "up", 100)
        ));
        when(healthAggregator.aggregateHealth()).thenReturn(Mono.just(expected));

        StepVerifier.create(controller.getServicesHealth())
                .expectNext(expected)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("ServiceHealthAggregator.aggregateHealth()를 호출한다")
    void should_delegateToAggregator_when_called() {
        var response = ServiceHealthResponse.of(List.of());
        when(healthAggregator.aggregateHealth()).thenReturn(Mono.just(response));

        controller.getServicesHealth().block();

        verify(healthAggregator, times(1)).aggregateHealth();
    }
}
