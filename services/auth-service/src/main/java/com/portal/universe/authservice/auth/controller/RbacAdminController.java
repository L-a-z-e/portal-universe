package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.rbac.*;
import com.portal.universe.authservice.auth.service.RbacService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/v1/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class RbacAdminController {

    private final RbacService rbacService;

    /**
     * 사용자 목록을 검색합니다 (email, username, nickname LIKE 또는 UUID exact match).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                rbacService.searchUsers(query, PageRequest.of(page - 1, size)))));
    }

    /**
     * 모든 활성 역할 목록을 조회합니다.
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getAllActiveRoles()));
    }

    /**
     * 역할 상세 정보를 권한 목록과 함께 조회합니다.
     */
    @GetMapping("/roles/{roleKey}")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleDetail(@PathVariable String roleKey) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getRoleDetail(roleKey)));
    }

    /**
     * 새 역할을 생성합니다.
     */
    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rbacService.createRole(request, adminId)));
    }

    /**
     * 역할의 displayName과 description을 수정합니다.
     */
    @PutMapping("/roles/{roleKey}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable String roleKey,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.updateRole(roleKey, request, adminId)));
    }

    /**
     * 역할의 활성/비활성 상태를 변경합니다.
     */
    @PatchMapping("/roles/{roleKey}/status")
    public ResponseEntity<ApiResponse<RoleResponse>> toggleRoleStatus(
            @PathVariable String roleKey,
            @RequestParam boolean active,
            @AuthenticationPrincipal String adminId) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.toggleRoleActive(roleKey, active, adminId)));
    }

    /**
     * 역할에 할당된 권한 목록을 조회합니다.
     */
    @GetMapping("/roles/{roleKey}/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(@PathVariable String roleKey) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getRolePermissions(roleKey)));
    }

    /**
     * 역할에 권한을 할당합니다.
     */
    @PostMapping("/roles/{roleKey}/permissions")
    public ResponseEntity<ApiResponse<Void>> assignPermissionToRole(
            @PathVariable String roleKey,
            @RequestParam String permissionKey,
            @AuthenticationPrincipal String adminId) {
        rbacService.assignPermissionToRole(roleKey, permissionKey, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 역할에서 권한을 해제합니다.
     */
    @DeleteMapping("/roles/{roleKey}/permissions/{permissionKey}")
    public ResponseEntity<ApiResponse<Void>> removePermissionFromRole(
            @PathVariable String roleKey,
            @PathVariable String permissionKey,
            @AuthenticationPrincipal String adminId) {
        rbacService.removePermissionFromRole(roleKey, permissionKey, adminId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 모든 활성 권한 목록을 조회합니다.
     */
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getAllActivePermissions()));
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

    /**
     * 대시보드 통계를 조회합니다.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(rbacService.getDashboardStats()));
    }

    /**
     * 전체 감사 로그를 페이징 조회합니다.
     */
    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                rbacService.getAuditLogs(PageRequest.of(page - 1, size)))));
    }

    /**
     * 특정 사용자의 감사 로그를 페이징 조회합니다.
     */
    @GetMapping("/users/{userId}/audit")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getUserAuditLogs(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(
                rbacService.getUserAuditLogs(userId, PageRequest.of(page - 1, size)))));
    }
}
