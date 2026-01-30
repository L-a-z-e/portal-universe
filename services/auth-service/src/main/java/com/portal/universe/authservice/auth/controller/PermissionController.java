package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.rbac.UserPermissionsResponse;
import com.portal.universe.authservice.auth.service.RbacService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 권한 조회 API 컨트롤러
 * 인증된 사용자가 자신의 권한을 조회할 수 있습니다.
 * 하위 서비스에서도 이 API를 호출하여 세밀한 권한 체크를 수행할 수 있습니다.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RbacService rbacService;

    /**
     * 내 전체 권한을 조회합니다 (역할 + 멤버십 기반).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserPermissionsResponse>> getMyPermissions(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(rbacService.resolveUserPermissions(userId)));
    }
}
