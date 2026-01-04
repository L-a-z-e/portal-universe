package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("FrontendProperties Test")
public class FrontendPropertiesTest {

    @Test
    @DisplayName("baseUrl이 파싱되어 host, scheme, port 설정되는지 확인")
    void init() {
        log.info("🧪 Test start!");

        // Given
        FrontendProperties frontendProperties = new FrontendProperties();
        String testBaseUrl = "https://portal-universe:30000";

        log.info("baseUrl: {}", testBaseUrl);

        // When
        frontendProperties.setBaseUrl(testBaseUrl);
        frontendProperties.init();

        log.debug("실행됨 - init() 메서드 호출");

        // Then
        log.info("☑️ 검증: scheme={}, host={}, port={}",
                frontendProperties.getScheme(),
                frontendProperties.getHost(),
                frontendProperties.getPort());

        assertThat(frontendProperties.getScheme()).isEqualTo("https");
        assertThat(frontendProperties.getHost()).contains("portal-universe");
        assertThat(frontendProperties.getPort()).isEqualTo(30000);

        log.info("✅ PASSED: init() 정상 동작");
    }

    @Test
    @DisplayName("getBaseUrl()이 기본값을 반환한다")
    void getBaseUrl() {
        // Given
        log.info("🧪 Test: getBaseUrl() 기본값 확인");
        FrontendProperties properties = new FrontendProperties();

        // When
        String result = properties.getBaseUrl();

        // Then
        log.info("결과: {}", result);
        assertThat(result).isEqualTo("http://localhost:30000");
        log.info("✅ PASSED");
    }

    @Test
    @DisplayName("getHost()가 파싱된 호스트를 반환한다")
    void getHost() {
        // Given
        log.info("🧪 Test: getHost() 파싱 확인");
        FrontendProperties properties = new FrontendProperties();
        properties.setBaseUrl("https://portal-universe:30000");

        // When
        properties.init();  // 파싱 실행
        String result = properties.getHost();

        // Then
        log.info("결과: {}", result);
        assertThat(result).contains("portal-universe");
        log.info("✅ PASSED");
    }

    @Test
    @DisplayName("getScheme()이 기본값을 반환한다")
    void getScheme() {
        // Given
        log.info("🧪 Test: getScheme() 기본값 확인");
        FrontendProperties properties = new FrontendProperties();

        // When
        String result = properties.getScheme();

        // Then
        log.info("결과: {}", result);
        assertThat(result).isEqualTo("http");
        log.info("✅ PASSED");
    }

    @Test
    @DisplayName("getPort()가 기본값을 반환한다")
    void getPort() {
        // Given
        log.info("🧪 Test: getPort() 기본값 확인");
        FrontendProperties properties = new FrontendProperties();

        // When
        int result = properties.getPort();

        // Then
        log.info("결과: {}", result);
        assertThat(result).isEqualTo(30000);
        log.info("✅ PASSED");
    }
}