package com.portal.universe.apigateway.controller;

import com.portal.universe.apigateway.exception.GatewayErrorCode;
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
    @DisplayName("authServiceFallback은 GW-F001 코드를 반환한다")
    void should_returnGWF001_when_authServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertErrorCode(result, GatewayErrorCode.AUTH_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("blogServiceFallback은 GW-F002 코드를 반환한다")
    void should_returnGWF002_when_blogServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.blogServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.BLOG_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("shoppingServiceFallback은 GW-F003 코드를 반환한다")
    void should_returnGWF003_when_shoppingServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.shoppingServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.SHOPPING_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("notificationServiceFallback은 GW-F004 코드를 반환한다")
    void should_returnGWF004_when_notificationServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.notificationServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.NOTIFICATION_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("driveServiceFallback은 GW-F007 코드를 반환한다")
    void should_returnGWF007_when_driveServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.driveServiceFallback().block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.DRIVE_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("prismServiceFallback은 GW-F008 코드를 반환한다")
    void should_returnGWF008_when_prismServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.prismServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.PRISM_SERVICE_UNAVAILABLE.getCode());
    }

    @Test
    @DisplayName("chatbotServiceFallback은 GW-F009 코드를 반환한다")
    void should_returnGWF009_when_chatbotServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.chatbotServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertErrorCode(result, GatewayErrorCode.CHATBOT_SERVICE_UNAVAILABLE.getCode());
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
    @DisplayName("fallback 응답의 data는 null이다")
    void should_containNullData_when_fallback() {
        ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

        assertThat(response).isNotNull();
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("data")).isNull();
    }

    @SuppressWarnings("unchecked")
    private void assertErrorCode(Map<String, Object> result, String expectedCode) {
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertThat(error).isNotNull();
        assertThat(error.get("code")).isEqualTo(expectedCode);
    }
}
