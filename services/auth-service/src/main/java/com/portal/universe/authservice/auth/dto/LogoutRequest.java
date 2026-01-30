package com.portal.universe.authservice.auth.dto;

/**
 * 로그아웃 요청 DTO
 * Cookie 우선, Body fallback이므로 refreshToken은 optional
 */
public record LogoutRequest(
        String refreshToken
) {
}
