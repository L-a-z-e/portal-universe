package com.portal.universe.authservice.user.dto.profile;

import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 프로필 조회 응답 DTO
 */
public record ProfileResponse(
        String uuid,
        String email,
        String nickname,
        String realName,
        String phoneNumber,
        String profileImageUrl,
        boolean marketingAgree,
        boolean hasSocialAccount,
        List<String> socialProviders,
        LocalDateTime createdAt
) {
    public static ProfileResponse from(User user) {
        UserProfile profile = user.getProfile();
        List<String> providers = user.getSocialAccounts().stream()
                .map(account -> account.getProvider().name())
                .toList();

        return new ProfileResponse(
                user.getUuid(),
                user.getEmail(),
                profile.getNickname(),
                profile.getRealName(),
                profile.getPhoneNumber(),
                profile.getProfileImageUrl(),
                profile.isMarketingAgree(),
                !user.getSocialAccounts().isEmpty(),
                providers,
                user.getCreatedAt()
        );
    }
}
