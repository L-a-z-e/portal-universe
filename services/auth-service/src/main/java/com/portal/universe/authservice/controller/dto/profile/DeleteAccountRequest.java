package com.portal.universe.authservice.controller.dto.profile;

import jakarta.validation.constraints.NotBlank;

/**
 * 회원 탈퇴 요청 DTO
 */
public record DeleteAccountRequest(
        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        String reason  // 탈퇴 사유 (선택)
) {
}
