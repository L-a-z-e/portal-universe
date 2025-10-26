package com.portal.universe.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Resilience4j 서킷 브레이커의 Fallback 응답을 처리하는 컨트롤러입니다.
 * 특정 마이크로서비스에 장애가 발생하여 서킷이 열렸을 때, 클라이언트에게 미리 정의된 응답을 반환합니다.
 */
@RestController
public class FallbackController {

    /**
     * Blog 서비스의 Fallback을 처리합니다.
     * Blog 서비스 호출이 실패하면 이 메서드가 대신 실행됩니다.
     * @return Fallback 응답 메시지를 담은 Mono<String>
     */
    @GetMapping("/fallback/blog")
    public Mono<String> blogServiceFallback() {
        return Mono.just("Blog Service is currently unavailable. Please try again later.");
    }

}