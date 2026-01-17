package com.portal.universe.shoppingservice.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Spring Cloud OpenFeign 클라이언트에 대한 전역 설정을 담당하는 클래스입니다.
 */
@Configuration
public class FeignClientConfig {

    /**
     * Feign 요청을 보내기 전에 실행되는 인터셉터(RequestInterceptor)를 Bean으로 등록합니다.
     * 이 인터셉터는 현재 요청의 'Authorization' 헤더(JWT 토큰)를 읽어,
     * Feign을 통해 나가는 요청에 그대로 복사해주는 역할을 합니다.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader("Authorization");

                if (Objects.nonNull(authorizationHeader)) {
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}
