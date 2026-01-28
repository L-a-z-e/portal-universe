package com.portal.universe.authservice.auth.dto.membership;

import jakarta.validation.constraints.NotBlank;

public record ChangeMembershipRequest(
        @NotBlank(message = "서비스 이름은 필수입니다")
        String serviceName,

        @NotBlank(message = "티어 키는 필수입니다")
        String tierKey
) {}
