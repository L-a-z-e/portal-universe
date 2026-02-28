package com.portal.universe.authservice.auth.dto.membership;

import com.portal.universe.authservice.auth.domain.MembershipStatus;
import com.portal.universe.authservice.auth.domain.UserMembership;

import java.time.Instant;

public record MembershipResponse(
        Long id,
        String userId,
        String membershipGroup,
        String tierKey,
        String tierDisplayName,
        MembershipStatus status,
        boolean autoRenew,
        Instant startedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static MembershipResponse from(UserMembership membership) {
        return new MembershipResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getMembershipGroup(),
                membership.getTier().getTierKey(),
                membership.getTier().getDisplayName(),
                membership.getStatus(),
                membership.isAutoRenew(),
                membership.getStartedAt(),
                membership.getExpiresAt(),
                membership.getCreatedAt()
        );
    }
}
