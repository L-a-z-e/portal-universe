package com.portal.universe.authservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO
 */
public record UserSignupRequest(
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 128, message = "비밀번호는 8자 이상 128자 이하여야 합니다")
        String password,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다")
        String nickname,

        @Size(max = 50, message = "실명은 50자 이하여야 합니다")
        String realName,

        boolean marketingAgree
) {
    public SignupCommand toCommand() {
        return new SignupCommand(email, password, nickname, realName, marketingAgree);
    }
}
