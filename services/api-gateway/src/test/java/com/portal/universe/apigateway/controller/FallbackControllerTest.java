package com.portal.universe.apigateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackController Test")
class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    @DisplayName("authServiceFallback은 GW001 코드를 반환한다")
    void should_returnGW001_when_authServiceFallback() {
        Map<String, Object> result = controller.authServiceFallback().block();

        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertErrorCode(result, "GW001");
    }

    @Test
    @DisplayName("blogServiceFallback은 GW002 코드를 반환한다")
    void should_returnGW002_when_blogServiceFallback() {
        Map<String, Object> result = controller.blogServiceFallback().block();

        assertThat(result).isNotNull();
        assertErrorCode(result, "GW002");
    }

    @Test
    @DisplayName("shoppingServiceFallback은 GW003 코드를 반환한다")
    void should_returnGW003_when_shoppingServiceFallback() {
        Map<String, Object> result = controller.shoppingServiceFallback().block();

        assertThat(result).isNotNull();
        assertErrorCode(result, "GW003");
    }

    @Test
    @DisplayName("notificationServiceFallback은 GW004 코드를 반환한다")
    void should_returnGW004_when_notificationServiceFallback() {
        Map<String, Object> result = controller.notificationServiceFallback().block();

        assertThat(result).isNotNull();
        assertErrorCode(result, "GW004");
    }

    @Test
    @DisplayName("fallback 응답에 timestamp가 포함된다")
    void should_containTimestamp_when_fallback() {
        Map<String, Object> result = controller.authServiceFallback().block();

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertThat(error.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("fallback 응답의 data는 빈 맵이다")
    void should_containEmptyData_when_fallback() {
        Map<String, Object> result = controller.authServiceFallback().block();

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
