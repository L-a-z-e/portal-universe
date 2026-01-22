package com.portal.universe.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발 및 디버깅 목적으로 사용되는 컨트롤러입니다.
 * 서비스의 상태를 확인하거나 예외 처리를 테스트하는 등의 용도로 사용됩니다.
 */
@RestController
public class DebugController {

    /**
     * 서비스가 정상적으로 실행 중인지 확인하기 위한 간단한 Ping-Pong 엔드포인트입니다.
     * Health check 용도로 프로덕션에서도 사용 가능합니다.
     *
     * @return "pong" 문자열
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    /**
     * ADMIN 역할이 필요한 API에 대한 접근 제어 테스트용 엔드포인트입니다.
     * SecurityConfig에서 ADMIN 권한이 필요하도록 설정되어 있어야 합니다.
     *
     * @return 성공 메시지
     */
    @GetMapping("/api/admin")
    public String adminEndpoint() {
        return "This is an admin endpoint.";
    }
}
