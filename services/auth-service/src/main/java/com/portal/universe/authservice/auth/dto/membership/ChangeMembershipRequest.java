package com.portal.universe.authservice.auth.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record ChangeMembershipRequest(
        @NotBlank(message = "멤버십 그룹은 필수입니다")
        String membershipGroup,

        @NotBlank(message = "티어 키는 필수입니다")
        String tierKey
) {}
