package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.rbac.*;
import com.portal.universe.authservice.auth.repository.*;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC(Role-Based Access Control) 핵심 비즈니스 로직을 담당합니다.
 * 역할 관리, 권한 조회, 사용자 역할 할당/해제를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RbacService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MembershipTierPermissionRepository membershipTierPermissionRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final AuthAuditLogRepository auditLogRepository;

    /**
     * 모든 활성 역할을 조회합니다.
     */
    public List<RoleResponse> getAllActiveRoles() {
        return roleEntityRepository.findByActiveTrue().stream()
                .map(RoleResponse::from)
                .toList();
    }

    /**
     * 사용자에게 할당된 역할 목록을 조회합니다.
     */
    public List<UserRoleResponse> getUserRoles(String userId) {
        return userRoleRepository.findByUserIdWithRole(userId).stream()
                .filter(ur -> !ur.isExpired())
                .map(UserRoleResponse::from)
                .toList();
    }

    /**
     * 사용자의 모든 권한을 해결(resolve)합니다.
     * 역할 기반 권한 + 멤버십 티어 기반 권한을 합산합니다.
     */
    public UserPermissionsResponse resolveUserPermissions(String userId) {
        // 1. 활성 역할 키 목록
        List<String> roleKeys = userRoleRepository.findActiveRoleKeysByUserId(userId);

        // 2. 역할 기반 권한 수집
        Set<String> permissions = new LinkedHashSet<>();
        if (!roleKeys.isEmpty()) {
            permissions.addAll(rolePermissionRepository.findPermissionKeysByRoleKeys(roleKeys));
        }

        // 3. 멤버십 티어 기반 권한 수집
        Map<String, String> memberships = new LinkedHashMap<>();
        List<UserMembership> activeMemberships = userMembershipRepository.findActiveByUserId(userId);
        for (UserMembership membership : activeMemberships) {
            String serviceName = membership.getServiceName();
            String tierKey = membership.getTier().getTierKey();
            memberships.put(serviceName, tierKey);

            List<String> tierPermissions = membershipTierPermissionRepository
                    .findPermissionKeysByServiceAndTier(serviceName, tierKey);
            permissions.addAll(tierPermissions);
        }

        return new UserPermissionsResponse(
                userId,
                roleKeys,
                new ArrayList<>(permissions),
                memberships
        );
    }

    /**
     * 사용자에게 역할을 할당합니다.
     */
    @Transactional
    public UserRoleResponse assignRole(AssignRoleRequest request, String assignedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(request.roleKey())
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        // 이미 할당된 역할인지 확인
        boolean alreadyAssigned = userRoleRepository.findByUserIdWithRole(request.userId()).stream()
                .anyMatch(ur -> ur.getRole().getRoleKey().equals(request.roleKey()) && !ur.isExpired());

        if (alreadyAssigned) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_ALREADY_ASSIGNED);
        }

        UserRole userRole = UserRole.builder()
                .userId(request.userId())
                .role(role)
                .assignedBy(assignedBy)
                .expiresAt(request.expiresAt())
                .build();

        UserRole saved = userRoleRepository.save(userRole);

        // 감사 로그
        logAudit(AuditEventType.ROLE_ASSIGNED, assignedBy, request.userId(),
                "Role assigned: " + request.roleKey());

        log.info("Role assigned: userId={}, role={}, by={}", request.userId(), request.roleKey(), assignedBy);
        return UserRoleResponse.from(saved);
    }

    /**
     * 사용자의 역할을 해제합니다.
     */
    @Transactional
    public void revokeRole(String userId, String roleKey, String revokedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        if (role.isSystem() && "ROLE_USER".equals(roleKey)) {
            throw new CustomBusinessException(AuthErrorCode.SYSTEM_ROLE_CANNOT_BE_MODIFIED);
        }

        UserRole userRole = userRoleRepository.findByUserIdWithRole(userId).stream()
                .filter(ur -> ur.getRole().getRoleKey().equals(roleKey))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_ASSIGNED));

        userRoleRepository.delete(userRole);

        logAudit(AuditEventType.ROLE_REVOKED, revokedBy, userId,
                "Role revoked: " + roleKey);

        log.info("Role revoked: userId={}, role={}, by={}", userId, roleKey, revokedBy);
    }

    private void logAudit(AuditEventType eventType, String actorId, String targetUserId, String details) {
        AuthAuditLog auditLog = AuthAuditLog.builder()
                .eventType(eventType)
                .actorUserId(actorId)
                .targetUserId(targetUserId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }
}
