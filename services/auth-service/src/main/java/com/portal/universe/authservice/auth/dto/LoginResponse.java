package com.portal.universe.authservice.auth.dto;

/**
 * 로그인 응답 DTO
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn  // 초 단위
) {
}
