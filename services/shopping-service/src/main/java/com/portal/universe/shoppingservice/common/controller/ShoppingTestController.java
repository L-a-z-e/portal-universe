package com.portal.universe.shoppingservice.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스가 정상적으로 실행 중인지 확인하기 위한 간단한 테스트용 컨트롤러입니다.
 */
@RestController
@RequestMapping("/test")
public class ShoppingTestController {
    @GetMapping
    public String test() {
        return "Shopping Service Test!";
    }
}
