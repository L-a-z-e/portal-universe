package com.portal.universe.authservice.controller.dto;

/**
 * Username 중복 확인 응답 DTO
 */
public record UsernameCheckResponse(
        String username,
        boolean available
) {}
