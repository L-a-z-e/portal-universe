package com.portal.universe.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate Limiting 응답 헤더 및 429 에러 응답을 커스터마이징하는 Global Filter입니다.
 *
 * 기능:
 * 1. 모든 응답에 Rate Limit 관련 헤더 추가
 *    - X-RateLimit-Remaining: 남은 요청 횟수
 *    - X-RateLimit-Replenish-Rate: 초당 토큰 충전 속도
 *    - X-RateLimit-Burst-Capacity: 최대 버스트 용량
 *
 * 2. 429 Too Many Requests 응답 커스터마이징
 *    - Retry-After 헤더 추가 (초 단위)
 *    - ApiResponse 형식의 통일된 에러 응답
 *
 * RedisRateLimiter는 기본적으로 다음 헤더를 추가합니다:
 * - X-RateLimit-Remaining
 * - X-RateLimit-Replenish-Rate
 * - X-RateLimit-Burst-Capacity
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitHeaderFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                // 응답 완료 후 로깅만 수행 (응답 스트림에 영향 없음)
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();

                String remaining = headers.getFirst("X-RateLimit-Remaining");
                if (remaining != null) {
                    log.debug("Rate Limit Info - Remaining: {}, Replenish Rate: {}, Burst Capacity: {}",
                        remaining,
                        headers.getFirst("X-RateLimit-Replenish-Rate"),
                        headers.getFirst("X-RateLimit-Burst-Capacity")
                    );
                }
            });
    }

    /**
     * 429 Too Many Requests 응답을 ApiResponse 형식으로 변환하고
     * Retry-After 헤더를 추가합니다.
     */
    private Mono<Void> handleTooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();

        // Retry-After 헤더 추가 (60초 후 재시도)
        String replenishRate = headers.getFirst("X-RateLimit-Replenish-Rate");
        int retryAfterSeconds = calculateRetryAfter(replenishRate);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));

        // ApiResponse 형식의 에러 응답 생성
        Map<String, Object> errorResponse = createErrorResponse(retryAfterSeconds);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);

            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            log.warn("Rate limit exceeded for request: {} {} | Retry after {} seconds",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                retryAfterSeconds
            );

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize rate limit error response", e);
            return Mono.empty();
        }
    }

    /**
     * Replenish Rate를 기반으로 Retry-After 시간 계산
     * replenishRate가 초당 1보다 작으면 1/rate로 계산
     * 그 외에는 기본 60초 반환
     */
    private int calculateRetryAfter(String replenishRate) {
        if (replenishRate != null) {
            try {
                double rate = Double.parseDouble(replenishRate);
                if (rate < 1 && rate > 0) {
                    // 예: rate=0.083 (5req/min) → 1/0.083 ≈ 12초
                    return (int) Math.ceil(1.0 / rate);
                }
            } catch (NumberFormatException e) {
                log.debug("Failed to parse replenish rate: {}", replenishRate);
            }
        }
        return 60; // 기본값: 60초
    }

    /**
     * ApiResponse 형식의 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(int retryAfterSeconds) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("data", null);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "TOO_MANY_REQUESTS");
        error.put("message", String.format(
            "요청 한도를 초과했습니다. %d초 후에 다시 시도해주세요.",
            retryAfterSeconds
        ));

        response.put("error", error);

        return response;
    }

    @Override
    public int getOrder() {
        // Rate Limit 필터 이후 실행 (낮은 우선순위)
        return Ordered.LOWEST_PRECEDENCE;
    }
}
