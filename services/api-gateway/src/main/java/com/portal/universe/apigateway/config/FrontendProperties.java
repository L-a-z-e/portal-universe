package com.portal.universe.apigateway.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.net.URI;

/**
 * 프론트엔드 관련 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "app.frontend")
@Data
@Component
@Slf4j
public class FrontendProperties {

    /**
     * 프론트엔드 전체 URL (http://localhost:30000)
     */
    private String baseUrl = "http://localhost:30000";

    /**
     * 호스트명 (포트 포함, 예: localhost:30000)
     */
    private String host;

    /**
     * 스킴 (http, https)
     */
    private String scheme = "http";

    /**
     * 포트
     */
    private int port = 30000;

    /**
     * 초기화 후 baseUrl에서 개별 값들 파싱
     */
    @PostConstruct
    public void init() {
        if (StringUtils.hasText(baseUrl)) {
            try {
                URI uri = URI.create(baseUrl);

                // host가 명시적으로 설정되지 않았으면 baseUrl에서 추출
                if (host == null) {
                    this.host = uri.getAuthority();  // host:port 형태
                }

                // scheme이 기본값이면 baseUrl에서 추출
                if ("http".equals(scheme)) {
                    this.scheme = uri.getScheme();
                }

                // port가 기본값이면 baseUrl에서 추출
                if (port == 30000) {
                    int uriPort = uri.getPort();
                    this.port = uriPort == -1 ?
                            ("https".equals(scheme) ? 443 : 80) : uriPort;
                }

                log.info("🔧 Frontend Properties initialized: baseUrl={}, host={}, scheme={}, port={}",
                        baseUrl, host, scheme, port);

            } catch (Exception e) {
                log.warn("⚠️ Failed to parse baseUrl: {}. Using defaults.", baseUrl);
            }
        }
    }
}
