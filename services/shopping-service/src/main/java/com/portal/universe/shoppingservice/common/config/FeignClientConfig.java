package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import feign.RequestInterceptor;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Spring Cloud OpenFeign 클라이언트에 대한 전역 설정을 담당하는 클래스입니다.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public MicrometerCapability micrometerCapability(MeterRegistry meterRegistry) {
        return new MicrometerCapability(meterRegistry);
    }

    private static final List<String> FORWARDED_HEADERS = List.of(
            AuthConstants.Headers.USER_ID,
            AuthConstants.Headers.USER_ROLES,
            AuthConstants.Headers.USER_EFFECTIVE_ROLES,
            AuthConstants.Headers.USER_MEMBERSHIPS,
            AuthConstants.Headers.USER_NICKNAME,
            AuthConstants.Headers.USER_NAME
    );

    /**
     * Feign 요청 인터셉터.
     * <p>
     * 1. 서비스 간 인증 토큰(X-Internal-Token)을 항상 추가 — HTTP/Kafka/Scheduler 무관
     * 2. HTTP 요청 컨텍스트가 있으면 X-User-* 헤더도 함께 전파 (감사 추적용)
     */
    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${app.internal.token}") String internalToken) {
        return requestTemplate -> {
            // 1. 서비스 간 통신 인증 토큰 (항상 추가)
            requestTemplate.header(AuthConstants.Headers.INTERNAL_TOKEN, internalToken);

            // 2. 사용자 컨텍스트 전파 (HTTP 요청이 있을 때만)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                for (String header : FORWARDED_HEADERS) {
                    String value = request.getHeader(header);
                    if (value != null) {
                        requestTemplate.header(header, value);
                    }
                }
            }
        };
    }
}
