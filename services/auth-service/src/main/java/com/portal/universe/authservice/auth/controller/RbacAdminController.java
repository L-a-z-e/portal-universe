package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.rbac.*;
import com.portal.universe.authservice.auth.service.RbacService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RBAC 관리 API 컨트롤러 (SUPER_ADMIN 전용)
 * 역할 조회, 사용자 역할 할당/해제, 사용자 권한 조회를 제공합니다.
 */
@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class RbacAdminController {

    private final RbacService rbacService;

    /**
     * 모든 활성 역할 목록을 조회합니다.
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getAllActiveRoles()));
    }

    /**
     * 사용자의 역할 목록을 조회합니다.
     */
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRoles(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getUserRoles(userId)));
    }

    /**
     * 사용자의 전체 권한을 조회합니다 (역할 + 멤버십 기반).
     */
    @GetMapping("/users/{userId}/permissions")
    public ResponseEntity<ApiResponse<UserPermissionsResponse>> getUserPermissions(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.resolveUserPermissions(userId)));
    }

    /**
     * 사용자에게 역할을 할당합니다.
     */
    @PostMapping("/roles/assign")
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rbacService.assignRole(request, adminId)));
    }

    /**
     * 사용자의 역할을 해제합니다.
     */
    @DeleteMapping("/users/{userId}/roles/{roleKey}")
    public ResponseEntity<ApiResponse<Void>> revokeRole(
            @PathVariable String userId,
            @PathVariable String roleKey,
            @AuthenticationPrincipal String adminId) {
        rbacService.revokeRole(userId, roleKey, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
