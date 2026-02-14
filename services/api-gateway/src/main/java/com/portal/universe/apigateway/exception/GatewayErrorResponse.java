package com.portal.universe.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gateway 전용 에러 응답 유틸리티.
 * WebFlux 환경에서 common-library ApiResponse를 사용할 수 없으므로
 * 동일한 형식의 에러 응답을 직접 생성한다.
 */
public final class GatewayErrorResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GatewayErrorResponse() {
    }

    /**
     * Filter에서 사용: ServerWebExchange에 에러 응답을 직접 쓴다.
     */
    public static Mono<Void> write(ServerWebExchange exchange, GatewayErrorCode errorCode) {
        exchange.getResponse().setStatusCode(errorCode.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = toBytes(errorCode.getCode(), errorCode.getMessage());
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Filter에서 사용: 커스텀 메시지로 에러 응답을 쓴다.
     */
    public static Mono<Void> write(ServerWebExchange exchange, GatewayErrorCode errorCode, String customMessage) {
        exchange.getResponse().setStatusCode(errorCode.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = toBytes(errorCode.getCode(), customMessage);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Controller에서 사용: ResponseEntity를 반환한다.
     */
    public static ResponseEntity<Map<String, Object>> of(GatewayErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(toMap(errorCode.getCode(), errorCode.getMessage()));
    }

    public static Map<String, Object> toMap(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("data", null);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().toString());
        response.put("error", error);

        return response;
    }

    private static byte[] toBytes(String code, String message) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(toMap(code, message));
        } catch (JsonProcessingException e) {
            String fallback = "{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                    + code + "\",\"message\":\"" + message + "\"}}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
