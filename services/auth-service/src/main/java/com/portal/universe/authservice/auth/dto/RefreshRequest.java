package com.portal.universe.authservice.auth.dto;

/**
 * 토큰 갱신 요청 DTO
 * Cookie 우선, Body fallback이므로 refreshToken은 optional
 */
public record RefreshRequest(
        String refreshToken
) {
}
