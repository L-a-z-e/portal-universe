package com.portal.universe.common.event;

public record UserSignedUpEvent(
        String userId,
        String email,
        String name
) {}