package com.portal.universe.shoppingservice.config;

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
     * 이 인터셉터는 현재 요청의 'Authorization' 헤더(JWT 토큰)를 읽어, Feign을 통해 나가는 요청에 그대로 복사해주는 역할을 합니다.
     *
     * ### 왜 필요한가?
     * MSA 환경에서 서비스 A가 서비스 B를 호출할 때, 서비스 B는 서비스 A로부터 온 요청이 정당한지 검증해야 합니다.
     * 이 때, 최초 사용자의 인증 정보(JWT)를 계속해서 전달(propagate)함으로써, 각 서비스가 독립적으로 요청을 인가할 수 있게 됩니다.
     * 이 인터셉터가 없으면, Feign 요청에는 인증 헤더가 포함되지 않아 호출받는 서비스에서 401 Unauthorized 오류가 발생합니다.
     *
     * @return RequestInterceptor Feign 요청 헤더를 조작하는 인터셉터
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 현재 스레드의 요청 속성을 가져옵니다.
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 현재 요청에서 "Authorization" 헤더를 추출합니다.
                String authorizationHeader = request.getHeader("Authorization");

                // 헤더가 존재하면, Feign 요청 템플릿에 동일한 헤더를 추가합니다.
                if (Objects.nonNull(authorizationHeader)) {
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}