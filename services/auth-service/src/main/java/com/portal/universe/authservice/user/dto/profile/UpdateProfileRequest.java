package com.portal.universe.authservice.user.dto.profile;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다")
        String nickname,

        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String realName,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
        String phoneNumber,

        @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다")
        String profileImageUrl,

        Boolean marketingAgree
) {
}
