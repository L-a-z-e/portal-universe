package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.membership.*;
import com.portal.universe.authservice.auth.service.MembershipService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 멤버십 관리 API 컨트롤러 (SUPER_ADMIN 전용)
 * 관리자가 사용자의 멤버십을 조회/변경할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/admin/memberships")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class MembershipAdminController {

    private final MembershipService membershipService;

    /**
     * 특정 사용자의 멤버십 목록을 조회합니다.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getUserMemberships(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMemberships(userId)));
    }

    /**
     * 특정 사용자의 멤버십 티어를 변경합니다.
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<MembershipResponse>> changeMembershipTier(
            @PathVariable String userId,
            @Valid @RequestBody ChangeMembershipRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.adminChangeMembershipTier(userId, request, adminId)));
    }
}
