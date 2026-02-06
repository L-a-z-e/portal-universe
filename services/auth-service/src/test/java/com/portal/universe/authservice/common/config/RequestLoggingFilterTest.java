package com.portal.universe.authservice.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Test")
class RequestLoggingFilterTest {

    @InjectMocks
    private RequestLoggingFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("일반 경로 요청 시 chain을 계속 진행한다")
    void should_continueChain_when_normalPath() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/v1/users/me");
        request.setMethod("GET");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("/actuator 경로는 로깅을 건너뛰고 chain을 계속 진행한다")
    void should_skipLoggingAndContinueChain_when_actuatorPath() throws ServletException, IOException {
        // given
        request.setRequestURI("/actuator/health");
        request.setMethod("GET");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("/actuator/prometheus 경로도 로깅을 건너뛴다")
    void should_skipLogging_when_actuatorPrometheusPath() throws ServletException, IOException {
        // given
        request.setRequestURI("/actuator/prometheus");
        request.setMethod("GET");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("POST 요청도 chain을 계속 진행한다")
    void should_continueChain_when_postRequest() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("X-Forwarded 헤더가 있는 요청도 chain을 계속 진행한다")
    void should_continueChain_when_requestHasForwardedHeaders() throws ServletException, IOException {
        // given
        request.setRequestURI("/api/v1/users/me");
        request.setMethod("GET");
        request.addHeader("Host", "portal-universe");
        request.addHeader("X-Forwarded-Host", "portal-universe");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Port", "443");
        request.addHeader("X-Forwarded-For", "192.168.1.100");

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }
}
