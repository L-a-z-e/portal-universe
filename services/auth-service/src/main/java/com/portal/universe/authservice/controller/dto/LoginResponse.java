package com.portal.universe.authservice.controller.dto;

/**
 * 로그인 응답 DTO
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn  // 초 단위
) {
}
