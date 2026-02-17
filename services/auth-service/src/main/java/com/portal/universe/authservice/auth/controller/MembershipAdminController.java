package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.membership.*;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingRequest;
import com.portal.universe.authservice.auth.dto.rbac.RoleDefaultMappingResponse;
import com.portal.universe.authservice.auth.service.MembershipService;
import com.portal.universe.authservice.auth.service.RoleDefaultMembershipService;
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
 * 관리자가 사용자의 멤버십을 조회/변경하고,
 * Role-Default Mapping과 Tier를 관리할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/admin/memberships")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class MembershipAdminController {

    private final MembershipService membershipService;
    private final RoleDefaultMembershipService roleDefaultMembershipService;

    // --- User Membership ---

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<String>>> getMembershipGroups() {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getAllMembershipGroups()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getUserMemberships(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getUserMemberships(userId)));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<MembershipResponse>> changeMembershipTier(
            @PathVariable String userId,
            @Valid @RequestBody ChangeMembershipRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(
                ApiResponse.success(membershipService.adminChangeMembershipTier(userId, request, adminId)));
    }

    // --- Role Default Mapping ---

    @GetMapping("/role-defaults")
    public ResponseEntity<ApiResponse<List<RoleDefaultMappingResponse>>> getAllRoleDefaults() {
        return ResponseEntity.ok(ApiResponse.success(roleDefaultMembershipService.getAllMappings()));
    }

    @GetMapping("/role-defaults/{roleKey}")
    public ResponseEntity<ApiResponse<List<RoleDefaultMappingResponse>>> getRoleDefaults(
            @PathVariable String roleKey) {
        return ResponseEntity.ok(ApiResponse.success(roleDefaultMembershipService.getMappingsByRoleKey(roleKey)));
    }

    @PostMapping("/role-defaults")
    public ResponseEntity<ApiResponse<RoleDefaultMappingResponse>> addRoleDefault(
            @Valid @RequestBody RoleDefaultMappingRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(roleDefaultMembershipService.addMapping(request, adminId)));
    }

    @DeleteMapping("/role-defaults/{roleKey}/{membershipGroup}")
    public ResponseEntity<ApiResponse<Void>> removeRoleDefault(
            @PathVariable String roleKey,
            @PathVariable String membershipGroup,
            @AuthenticationPrincipal String adminId) {
        roleDefaultMembershipService.removeMapping(roleKey, membershipGroup, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // --- Tier CRUD ---

    @PostMapping("/tiers")
    public ResponseEntity<ApiResponse<MembershipTierResponse>> createTier(
            @Valid @RequestBody CreateMembershipTierRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.createTier(request, adminId)));
    }

    @PutMapping("/tiers/{tierId}")
    public ResponseEntity<ApiResponse<MembershipTierResponse>> updateTier(
            @PathVariable Long tierId,
            @Valid @RequestBody UpdateMembershipTierRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.updateTier(tierId, request, adminId)));
    }

    @DeleteMapping("/tiers/{tierId}")
    public ResponseEntity<ApiResponse<Void>> deleteTier(
            @PathVariable Long tierId,
            @AuthenticationPrincipal String adminId) {
        membershipService.deleteTier(tierId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
