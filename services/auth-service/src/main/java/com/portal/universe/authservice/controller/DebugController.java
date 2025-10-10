package com.portal.universe.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugController {

    @GetMapping("/ping")
    public String ping() {
        // 이 컨트롤러가 동작하는지 확인하기 위한 간단한 엔드포인트
        return "pong";
    }

    @GetMapping("/force-error")
    public String forceError() {
        // GlobalExceptionHandler가 예외를 제대로 잡고 스택 트레이스를 출력하는지 테스트하기 위한 엔드포인트
        throw new RuntimeException("This is a forced test exception.");
    }
}