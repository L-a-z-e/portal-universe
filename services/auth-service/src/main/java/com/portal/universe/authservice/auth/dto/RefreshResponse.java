package com.portal.universe.authservice.auth.dto;

/**
 * 토큰 갱신 응답 DTO
 */
public record RefreshResponse(
        String accessToken,
        long expiresIn  // 초 단위
) {
}
