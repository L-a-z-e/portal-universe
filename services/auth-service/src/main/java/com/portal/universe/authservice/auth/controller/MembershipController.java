package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.membership.*;
import com.portal.universe.authservice.auth.service.MembershipService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 멤버십 사용자 셀프서비스 API 컨트롤러
 * 인증된 사용자가 자신의 멤버십을 조회/변경/취소할 수 있습니다.
 */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    /**
     * 내 멤버십 목록을 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyMemberships(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMemberships(userId)));
    }

    /**
     * 내 특정 서비스 멤버십을 조회합니다.
     */
    @GetMapping("/me/{serviceName}")
    public ResponseEntity<ApiResponse<MembershipResponse>> getMyMembership(
            @AuthenticationPrincipal String userId,
            @PathVariable String serviceName) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMembership(userId, serviceName)));
    }

    /**
     * 서비스별 사용 가능한 멤버십 티어를 조회합니다 (공개).
     */
    @GetMapping("/tiers/{serviceName}")
    public ResponseEntity<ApiResponse<List<MembershipTierResponse>>> getServiceTiers(
            @PathVariable String serviceName) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getServiceTiers(serviceName)));
    }

    /**
     * 내 멤버십 티어를 변경합니다.
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MembershipResponse>> changeMembershipTier(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangeMembershipRequest request) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.changeMembershipTier(userId, request)));
    }

    /**
     * 내 멤버십을 취소합니다 (FREE 티어로 복귀).
     */
    @DeleteMapping("/me/{serviceName}")
    public ResponseEntity<ApiResponse<Void>> cancelMembership(
            @AuthenticationPrincipal String userId,
            @PathVariable String serviceName) {
        membershipService.cancelMembership(userId, serviceName);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
