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

@RestController
@RequestMapping("/api/v1/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyMemberships(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMemberships(userId)));
    }

    @GetMapping("/me/{membershipGroup}")
    public ResponseEntity<ApiResponse<MembershipResponse>> getMyMembership(
            @AuthenticationPrincipal String userId,
            @PathVariable String membershipGroup) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMembership(userId, membershipGroup)));
    }

    @GetMapping("/tiers/{membershipGroup}")
    public ResponseEntity<ApiResponse<List<MembershipTierResponse>>> getGroupTiers(
            @PathVariable String membershipGroup) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getGroupTiers(membershipGroup)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MembershipResponse>> changeMembershipTier(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangeMembershipRequest request) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.changeMembershipTier(userId, request)));
    }

    @DeleteMapping("/me/{membershipGroup}")
    public ResponseEntity<ApiResponse<Void>> cancelMembership(
            @AuthenticationPrincipal String userId,
            @PathVariable String membershipGroup) {
        membershipService.cancelMembership(userId, membershipGroup);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
