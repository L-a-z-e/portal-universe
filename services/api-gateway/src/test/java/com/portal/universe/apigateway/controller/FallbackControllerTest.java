package com.portal.universe.apigateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackController Test")
class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    @DisplayName("authServiceFallback은 GW001 코드를 반환한다")
    void should_returnGW001_when_authServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertErrorCode(result, "GW001");
    }

    @Test
    @DisplayName("blogServiceFallback은 GW002 코드를 반환한다")
    void should_returnGW002_when_blogServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.blogServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, "GW002");
    }

    @Test
    @DisplayName("shoppingServiceFallback은 GW003 코드를 반환한다")
    void should_returnGW003_when_shoppingServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.shoppingServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, "GW003");
    }

    @Test
    @DisplayName("notificationServiceFallback은 GW004 코드를 반환한다")
    void should_returnGW004_when_notificationServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.notificationServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, "GW004");
    }

    @Test
    @DisplayName("fallback 응답에 timestamp가 포함된다")
    void should_containTimestamp_when_fallback() {
        ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertThat(error.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("fallback 응답의 data는 빈 맵이다")
    void should_containEmptyData_when_fallback() {
        ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertThat(data).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void assertErrorCode(Map<String, Object> result, String expectedCode) {
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertThat(error).isNotNull();
        assertThat(error.get("code")).isEqualTo(expectedCode);
    }
}
