package com.portal.universe.authservice.controller.dto;

/**
 * 토큰 갱신 응답 DTO
 */
public record RefreshResponse(
        String accessToken,
        long expiresIn  // 초 단위
) {
}
