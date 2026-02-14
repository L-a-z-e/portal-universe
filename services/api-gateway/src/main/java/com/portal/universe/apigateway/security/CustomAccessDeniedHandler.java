package com.portal.universe.apigateway.security;

import com.portal.universe.apigateway.exception.GatewayErrorCode;
import com.portal.universe.apigateway.exception.GatewayErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 권한이 없는 요청에 대한 커스텀 핸들러.
 *
 * <p>JSON 형식의 API 응답을 반환합니다.</p>
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.debug("Access denied: {}", denied.getMessage());
        return GatewayErrorResponse.write(exchange, GatewayErrorCode.ACCESS_DENIED);
    }
}
