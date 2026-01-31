package com.portal.universe.apigateway.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Resilience4j 서킷 브레이커의 Fallback 응답을 처리하는 컨트롤러입니다.
 * 특정 마이크로서비스에 장애가 발생하여 서킷이 열렸을 때, 클라이언트에게 ApiResponse 형식의 응답을 반환합니다.
 */
@RestController
public class FallbackController {

    @GetMapping(value = "/fallback/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> authServiceFallback() {
        return Mono.just(buildFallbackResponse("GW001", "인증 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    @GetMapping(value = "/fallback/blog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> blogServiceFallback() {
        return Mono.just(buildFallbackResponse("GW002", "블로그 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    @GetMapping(value = "/fallback/shopping", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> shoppingServiceFallback() {
        return Mono.just(buildFallbackResponse("GW003", "쇼핑 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    @GetMapping(value = "/fallback/notification", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> notificationServiceFallback() {
        return Mono.just(buildFallbackResponse("GW004", "알림 서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."));
    }

    private Map<String, Object> buildFallbackResponse(String code, String message) {
        return Map.of(
                "success", false,
                "data", Map.of(),
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString()
                )
        );
    }
}
