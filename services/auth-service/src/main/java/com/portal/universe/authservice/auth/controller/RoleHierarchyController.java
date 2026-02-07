package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.service.RoleHierarchyService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gateway에서 호출하는 내부 API.
 * JWT의 역할 목록을 받아 Role Hierarchy 상속을 적용한 전체 유효 역할을 반환합니다.
 */
@RestController
@RequestMapping("/api/v1/internal/role-hierarchy")
@RequiredArgsConstructor
public class RoleHierarchyController {

    private final RoleHierarchyService roleHierarchyService;

    /**
     * 주어진 역할 목록에 대해 상속된 모든 유효 역할을 반환합니다.
     *
     * @param roles 쉼표 구분 역할 키 (예: "ROLE_SHOPPING_ADMIN,ROLE_USER")
     * @return 상속 포함 전체 유효 역할 키 목록
     */
    @GetMapping("/effective-roles")
    public ResponseEntity<ApiResponse<List<String>>> getEffectiveRoles(
            @RequestParam List<String> roles) {
        List<String> effectiveRoles = roleHierarchyService.resolveEffectiveRoles(roles);
        return ResponseEntity.ok(ApiResponse.success(effectiveRoles));
    }
}
