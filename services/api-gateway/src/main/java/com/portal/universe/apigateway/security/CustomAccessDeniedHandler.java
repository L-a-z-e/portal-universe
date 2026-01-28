package com.portal.universe.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 권한이 없는 요청에 대한 커스텀 핸들러.
 *
 * <p>JSON 형식의 API 응답을 반환합니다.</p>
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {

    private static final String FORBIDDEN_RESPONSE = """
            {"success":false,"data":null,"error":{"code":"A002","message":"Access denied"}}""";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        log.debug("Access denied: {}", denied.getMessage());

        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = FORBIDDEN_RESPONSE.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
