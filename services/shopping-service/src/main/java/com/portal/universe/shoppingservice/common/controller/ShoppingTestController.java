package com.portal.universe.shoppingservice.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스가 정상적으로 실행 중인지 확인하기 위한 간단한 테스트용 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/shopping")
public class ShoppingTestController {
    /**
     * "Shopping Service Test!" 문자열을 반환하여 서비스 동작을 확인합니다.
     * @return 테스트 문자열
     */
    @GetMapping
    public String test() {
        return "Shopping Service Test!";
    }
}
