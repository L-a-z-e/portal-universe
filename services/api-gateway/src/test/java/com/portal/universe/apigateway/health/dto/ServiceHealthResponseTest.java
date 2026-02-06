package com.portal.universe.apigateway.health.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceHealthResponse Test")
class ServiceHealthResponseTest {

    @Test
    @DisplayName("모든 서비스가 up이면 overallStatus는 up이다")
    void should_returnUp_when_allServicesAreUp() {
        var services = List.of(
                ServiceHealthInfo.of("auth", "Auth Service", "up", 100),
                ServiceHealthInfo.of("blog", "Blog Service", "up", 50),
                ServiceHealthInfo.of("shopping", "Shopping Service", "up", 80)
        );

        var response = ServiceHealthResponse.of(services);

        assertThat(response.overallStatus()).isEqualTo("up");
    }

    @Test
    @DisplayName("모든 서비스가 down이면 overallStatus는 down이다")
    void should_returnDown_when_allServicesAreDown() {
        var services = List.of(
                ServiceHealthInfo.of("auth", "Auth Service", "down", 3000),
                ServiceHealthInfo.of("blog", "Blog Service", "down", 3000)
        );

        var response = ServiceHealthResponse.of(services);

        assertThat(response.overallStatus()).isEqualTo("down");
    }

    @Test
    @DisplayName("일부 서비스만 up이면 overallStatus는 degraded이다")
    void should_returnDegraded_when_mixedStatus() {
        var services = List.of(
                ServiceHealthInfo.of("auth", "Auth Service", "up", 100),
                ServiceHealthInfo.of("blog", "Blog Service", "down", 3000)
        );

        var response = ServiceHealthResponse.of(services);

        assertThat(response.overallStatus()).isEqualTo("degraded");
    }

    @Test
    @DisplayName("서비스 목록이 비어있으면 overallStatus는 unknown이다")
    void should_returnUnknown_when_emptyServices() {
        var response = ServiceHealthResponse.of(Collections.emptyList());

        assertThat(response.overallStatus()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("timestamp가 null이 아니다")
    void should_containTimestamp() {
        var response = ServiceHealthResponse.of(List.of(
                ServiceHealthInfo.of("auth", "Auth Service", "up", 100)
        ));

        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("모든 서비스 정보를 포함한다")
    void should_containAllServices() {
        var services = List.of(
                ServiceHealthInfo.of("auth", "Auth", "up", 100),
                ServiceHealthInfo.of("blog", "Blog", "up", 50),
                ServiceHealthInfo.of("shop", "Shop", "down", 3000)
        );

        var response = ServiceHealthResponse.of(services);

        assertThat(response.services()).hasSize(3);
    }
}
