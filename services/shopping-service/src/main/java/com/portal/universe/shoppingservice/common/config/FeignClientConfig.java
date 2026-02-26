package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
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

    private static final List<String> FORWARDED_HEADERS = List.of(
            AuthConstants.Headers.USER_ID,
            AuthConstants.Headers.USER_ROLES,
            AuthConstants.Headers.USER_EFFECTIVE_ROLES,
            AuthConstants.Headers.USER_MEMBERSHIPS,
            AuthConstants.Headers.USER_NICKNAME,
            AuthConstants.Headers.USER_NAME
    );

    /**
     * Feign 요청을 보내기 전에 실행되는 인터셉터(RequestInterceptor)를 Bean으로 등록합니다.
     * Gateway가 설정한 X-User-* 헤더를 Feign 요청에 그대로 전파하여,
     * 호출 대상 서비스의 GatewayAuthenticationFilter가 정상 동작하도록 합니다.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
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
