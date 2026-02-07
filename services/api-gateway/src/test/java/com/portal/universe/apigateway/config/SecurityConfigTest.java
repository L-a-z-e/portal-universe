package com.portal.universe.apigateway.config;

import com.portal.universe.apigateway.security.CustomAccessDeniedHandler;
import com.portal.universe.apigateway.security.CustomAuthenticationEntryPoint;
import com.portal.universe.apigateway.service.RoleHierarchyResolver;
import com.portal.universe.apigateway.service.TokenBlacklistChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Test")
class SecurityConfigTest {

    @Mock
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Mock
    private TokenBlacklistChecker tokenBlacklistChecker;

    @Mock
    private RoleHierarchyResolver roleHierarchyResolver;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        var jwtProperties = new JwtProperties();
        jwtProperties.setCurrentKeyId("test-key");
        var keyConfig = new JwtProperties.KeyConfig();
        keyConfig.setSecretKey("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
        jwtProperties.setKeys(java.util.Map.of("test-key", keyConfig));

        var publicPathProperties = new PublicPathProperties();
        publicPathProperties.setPermitAll(List.of("/actuator/**", "/fallback/**"));
        publicPathProperties.setPermitAllGet(List.of("/api/v1/blog/**"));
        publicPathProperties.setSkipJwtParsing(List.of("/actuator", "/fallback"));

        securityConfig = new SecurityConfig(
                jwtProperties, publicPathProperties,
                authenticationEntryPoint, accessDeniedHandler, tokenBlacklistChecker,
                roleHierarchyResolver
        );
    }

    @Test
    @DisplayName("JwtAuthenticationFilter Bean을 생성할 수 있다")
    void should_createJwtAuthenticationFilter_bean() {
        var filter = securityConfig.jwtAuthenticationFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("CorsWebFilter Bean을 생성할 수 있다")
    void should_createCorsWebFilter_bean() {
        var corsFilter = securityConfig.corsWebFilter();

        assertThat(corsFilter).isNotNull();
    }

    @Test
    @DisplayName("RequestPathLoggingFilter Bean을 생성할 수 있다")
    void should_createRequestPathLoggingFilter_bean() {
        WebFilter loggingFilter = securityConfig.requestPathLoggingFilter();

        assertThat(loggingFilter).isNotNull();
    }

    @Test
    @DisplayName("RequestPathLoggingFilter가 정상 동작한다")
    void should_logRequestPath_when_requestReceived() {
        WebFilter loggingFilter = securityConfig.requestPathLoggingFilter();
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(org.springframework.web.server.WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(loggingFilter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(chain).filter(exchange);
    }
}
