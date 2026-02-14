package com.portal.universe.apigateway.security;

import com.portal.universe.apigateway.exception.GatewayErrorCode;
import com.portal.universe.apigateway.exception.GatewayErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 인증되지 않은 요청에 대한 커스텀 엔트리 포인트.
 *
 * <p>Spring Security 기본 동작(WWW-Authenticate 헤더 전송)을 방지하여
 * 브라우저의 Basic Auth 팝업이 나타나지 않도록 합니다.</p>
 *
 * <p>대신 JSON 형식의 API 응답을 반환합니다.</p>
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return GatewayErrorResponse.write(exchange, GatewayErrorCode.AUTHENTICATION_REQUIRED);
    }
}
