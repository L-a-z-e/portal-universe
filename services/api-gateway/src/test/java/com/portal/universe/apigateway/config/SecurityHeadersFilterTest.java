package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SecurityHeadersFilter Test")
class SecurityHeadersFilterTest {

    private SecurityHeadersProperties properties;
    private GatewayFilterChain chain;
    private SecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        properties = new SecurityHeadersProperties();
        chain = mock(GatewayFilterChain.class);
        filter = new SecurityHeadersFilter(properties);
    }

    private HttpHeaders executeFilterAndGetHeaders(MockServerHttpRequest request) {
        var exchange = MockServerWebExchange.from(request);
        // chain.filter가 완료되면 beforeCommit 콜백이 트리거됨
        when(chain.filter(any())).thenAnswer(inv -> {
            // beforeCommit 콜백을 트리거하기 위해 setComplete 호출
            return exchange.getResponse().setComplete().then(Mono.empty());
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        return exchange.getResponse().getHeaders();
    }

    @Test
    @DisplayName("비활성화 시 보안 헤더를 추가하지 않는다")
    void should_skipHeaders_when_disabled() {
        properties.setEnabled(false);
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getHeaders().get("X-Content-Type-Options")).isNull();
    }

    @Nested
    @DisplayName("기본 보안 헤더")
    class BasicHeaders {

        @Test
        @DisplayName("X-Content-Type-Options: nosniff를 추가한다")
        void should_addContentTypeOptions_when_enabled() {
            properties.setContentTypeOptions(true);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        }

        @Test
        @DisplayName("X-Frame-Options를 추가한다")
        void should_addFrameOptions_when_set() {
            properties.setFrameOptions("DENY");
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        }

        @Test
        @DisplayName("X-XSS-Protection을 추가한다")
        void should_addXssProtection_when_enabled() {
            properties.setXssProtection(true);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
        }

        @Test
        @DisplayName("Referrer-Policy를 추가한다")
        void should_addReferrerPolicy_when_set() {
            properties.setReferrerPolicy("strict-origin-when-cross-origin");
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        }

        @Test
        @DisplayName("Permissions-Policy를 추가한다")
        void should_addPermissionsPolicy_when_set() {
            properties.setPermissionsPolicy("geolocation=(), microphone=(), camera=()");
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Permissions-Policy")).isEqualTo("geolocation=(), microphone=(), camera=()");
        }
    }

    @Nested
    @DisplayName("CSP 헤더")
    class CspHeaders {

        @Test
        @DisplayName("CSP가 활성화되면 Content-Security-Policy를 추가한다")
        void should_addCsp_when_enabled() {
            properties.getCsp().setEnabled(true);
            properties.getCsp().setPolicy("default-src 'self'");
            properties.getCsp().setReportOnly(false);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Content-Security-Policy")).isEqualTo("default-src 'self'");
        }

        @Test
        @DisplayName("Report-Only 모드에서는 Report-Only 헤더를 사용한다")
        void should_addCspReportOnly_when_reportOnlyMode() {
            properties.getCsp().setEnabled(true);
            properties.getCsp().setPolicy("default-src 'self'");
            properties.getCsp().setReportOnly(true);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Content-Security-Policy-Report-Only")).isEqualTo("default-src 'self'");
            assertThat(headers.getFirst("Content-Security-Policy")).isNull();
        }

        @Test
        @DisplayName("CSP가 비활성화되면 헤더를 추가하지 않는다")
        void should_notAddCsp_when_disabled() {
            properties.getCsp().setEnabled(false);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Content-Security-Policy")).isNull();
            assertThat(headers.getFirst("Content-Security-Policy-Report-Only")).isNull();
        }
    }

    @Nested
    @DisplayName("HSTS 헤더")
    class HstsHeaders {

        @Test
        @DisplayName("HTTPS 요청에 HSTS 헤더를 추가한다")
        void should_addHsts_when_httpsRequest() {
            properties.getHsts().setEnabled(true);
            properties.getHsts().setMaxAge(31536000);
            properties.getHsts().setIncludeSubDomains(true);
            properties.getHsts().setHttpsOnly(true);
            var headers = executeFilterAndGetHeaders(
                    MockServerHttpRequest.get("/api/test").header("X-Forwarded-Proto", "https").build());

            assertThat(headers.getFirst("Strict-Transport-Security"))
                    .contains("max-age=31536000")
                    .contains("includeSubDomains");
        }

        @Test
        @DisplayName("HTTP 요청에는 HSTS 헤더를 추가하지 않는다 (httpsOnly)")
        void should_notAddHsts_when_httpRequest() {
            properties.getHsts().setEnabled(true);
            properties.getHsts().setHttpsOnly(true);
            var headers = executeFilterAndGetHeaders(MockServerHttpRequest.get("/api/test").build());

            assertThat(headers.getFirst("Strict-Transport-Security")).isNull();
        }

        @Test
        @DisplayName("HSTS가 비활성화되면 헤더를 추가하지 않는다")
        void should_notAddHsts_when_disabled() {
            properties.getHsts().setEnabled(false);
            var headers = executeFilterAndGetHeaders(
                    MockServerHttpRequest.get("/api/test").header("X-Forwarded-Proto", "https").build());

            assertThat(headers.getFirst("Strict-Transport-Security")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache-Control 헤더")
    class CacheControlHeaders {

        @Test
        @DisplayName("인증 경로에 Cache-Control을 추가한다")
        void should_addCacheControl_when_authPath() {
            properties.getCacheControl().setAuthPaths(true);
            properties.getCacheControl().setNoCachePaths(new String[]{"/api/auth/**"});
            var headers = executeFilterAndGetHeaders(
                    MockServerHttpRequest.get("/api/auth/login").build());

            assertThat(headers.getFirst("Cache-Control")).contains("no-store");
            assertThat(headers.getFirst("Pragma")).isEqualTo("no-cache");
        }

        @Test
        @DisplayName("비인증 경로에는 Cache-Control을 추가하지 않는다")
        void should_notAddCacheControl_when_nonAuthPath() {
            properties.getCacheControl().setAuthPaths(true);
            properties.getCacheControl().setNoCachePaths(new String[]{"/api/auth/**"});
            var headers = executeFilterAndGetHeaders(
                    MockServerHttpRequest.get("/api/blog/posts").build());

            assertThat(headers.getFirst("Cache-Control")).isNull();
        }
    }
}
