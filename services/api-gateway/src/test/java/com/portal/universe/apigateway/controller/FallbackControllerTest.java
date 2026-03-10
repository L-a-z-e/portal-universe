package com.portal.universe.apigateway.controller;

import com.portal.universe.apigateway.exception.GatewayErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackController")
class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Nested
    @DisplayName("service fallbacks")
    class ServiceFallbacks {

        @Test
        @DisplayName("should_returnGWF001_when_authServiceFallback")
        void should_returnGWF001_when_authServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.AUTH_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF002_when_blogServiceFallback")
        void should_returnGWF002_when_blogServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.blogServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.BLOG_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF003_when_shoppingServiceFallback")
        void should_returnGWF003_when_shoppingServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.shoppingServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.SHOPPING_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF004_when_notificationServiceFallback")
        void should_returnGWF004_when_notificationServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.notificationServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.NOTIFICATION_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF007_when_driveServiceFallback")
        void should_returnGWF007_when_driveServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.driveServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.DRIVE_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF008_when_prismServiceFallback")
        void should_returnGWF008_when_prismServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.prismServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.PRISM_SERVICE_UNAVAILABLE.getCode());
        }

        @Test
        @DisplayName("should_returnGWF009_when_chatbotServiceFallback")
        void should_returnGWF009_when_chatbotServiceFallback() {
            ResponseEntity<Map<String, Object>> response = controller.chatbotServiceFallback().block();

            assertFallbackResponse(response, GatewayErrorCode.CHATBOT_SERVICE_UNAVAILABLE.getCode());
        }
    }

    @Nested
    @DisplayName("response structure")
    class ResponseStructure {

        @Test
        @DisplayName("should_containTimestamp_when_fallback")
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
        @DisplayName("should_containNullData_when_fallback")
        void should_containNullData_when_fallback() {
            ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

            assertThat(response).isNotNull();
            Map<String, Object> result = response.getBody();
            assertThat(result).isNotNull();
            assertThat(result.get("data")).isNull();
        }

        @Test
        @DisplayName("should_return503Status_when_fallback")
        void should_return503Status_when_fallback() {
            ResponseEntity<Map<String, Object>> response = controller.authServiceFallback().block();

            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    // ── Helper ──

    @SuppressWarnings("unchecked")
    private void assertFallbackResponse(ResponseEntity<Map<String, Object>> response, String expectedCode) {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        Map<String, Object> error = (Map<String, Object>) result.get("error");
        assertThat(error).isNotNull();
        assertThat(error.get("code")).isEqualTo(expectedCode);
    }
}
