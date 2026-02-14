package com.portal.universe.apigateway.controller;

import com.portal.universe.apigateway.exception.GatewayErrorCode;
import com.portal.universe.apigateway.exception.GatewayErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Resilience4j 서킷 브레이커의 Fallback 응답을 처리하는 컨트롤러입니다.
 * 특정 마이크로서비스에 장애가 발생하여 서킷이 열렸을 때, 클라이언트에게 ApiResponse 형식의 응답을 반환합니다.
 *
 * 모든 HTTP 메서드를 지원합니다 (GET, POST, PUT, DELETE 등).
 * Circuit Breaker는 원본 요청의 메서드를 그대로 전달하므로 모든 메서드를 처리해야 합니다.
 */
@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.AUTH_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/blog", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> blogServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.BLOG_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/shopping", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> shoppingServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.SHOPPING_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/notification", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.NOTIFICATION_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/shopping-seller", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> shoppingSellerServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.SHOPPING_SELLER_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/shopping-settlement", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> shoppingSettlementServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.SHOPPING_SETTLEMENT_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/drive", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> driveServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.DRIVE_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/prism", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> prismServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.PRISM_SERVICE_UNAVAILABLE));
    }

    @RequestMapping(value = "/fallback/chatbot", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> chatbotServiceFallback() {
        return Mono.just(GatewayErrorResponse.of(GatewayErrorCode.CHATBOT_SERVICE_UNAVAILABLE));
    }
}
