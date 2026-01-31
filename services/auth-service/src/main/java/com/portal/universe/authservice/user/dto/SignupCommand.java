package com.portal.universe.authservice.user.dto;

/**
 * 회원가입 요청 Command DTO
 * Controller에서 Service로 전달되는 데이터를 담습니다.
 */
public record SignupCommand(
        String email,
        String password,
        String nickname,
        String realName,
        boolean marketingAgree
) {}
