package com.portal.universe.authservice.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh Token은 필수입니다")
        String refreshToken
) {
}
