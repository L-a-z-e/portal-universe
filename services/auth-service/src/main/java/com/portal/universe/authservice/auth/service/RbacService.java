package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.dto.rbac.*;
import com.portal.universe.authservice.auth.repository.*;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.user.domain.UserStatus;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
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
    private final PermissionRepository permissionRepository;
    private final RoleIncludeRepository roleIncludeRepository;
    private final RoleHierarchyService roleHierarchyService;
    private final MembershipTierPermissionRepository membershipTierPermissionRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final AuthAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-.*");

    /**
     * 사용자를 검색합니다.
     * query가 비어있으면 전체 목록, UUID 패턴이면 exact match, 그 외 LIKE 검색.
     */
    public Page<AdminUserResponse> searchUsers(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return userRepository.findAllBy(pageable).map(AdminUserResponse::from);
        }
        if (UUID_PATTERN.matcher(query.trim()).matches()) {
            return userRepository.findByUuidWithProfile(query.trim())
                    .map(u -> (Page<AdminUserResponse>) new PageImpl<>(List.of(AdminUserResponse.from(u)), pageable, 1))
                    .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
        }
        return userRepository.searchByQuery(query.trim(), pageable).map(AdminUserResponse::from);
    }

    /**
     * 모든 활성 역할을 조회합니다.
     */
    public List<RoleResponse> getAllActiveRoles() {
        List<RoleEntity> roles = roleEntityRepository.findByActiveTrue();
        List<RoleInclude> allIncludes = roleIncludeRepository.findAllWithRoles();
        Map<Long, List<RoleInclude>> includesByRoleId = allIncludes.stream()
                .collect(Collectors.groupingBy(ri -> ri.getRole().getId()));
        return roles.stream()
                .map(role -> RoleResponse.from(role, includesByRoleId.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    /**
     * 역할 상세 정보를 권한 목록, includes, effectiveRoles와 함께 조회합니다.
     */
    public RoleDetailResponse getRoleDetail(String roleKey) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        List<RoleInclude> includes = roleIncludeRepository.findByRole(role);
        List<String> effectiveRoleKeys = roleHierarchyService.resolveEffectiveRoles(List.of(roleKey));
        List<PermissionEntity> permissions = rolePermissionRepository.findByRole(role).stream()
                .map(RolePermission::getPermission)
                .toList();
        return RoleDetailResponse.from(role, includes, effectiveRoleKeys, permissions);
    }

    /**
     * 새 역할을 생성합니다.
     */
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String createdBy) {
        if (roleEntityRepository.existsByRoleKey(request.roleKey())) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_KEY_ALREADY_EXISTS);
        }

        RoleEntity role = RoleEntity.builder()
                .roleKey(request.roleKey())
                .displayName(request.displayName())
                .description(request.description())
                .serviceScope(request.serviceScope())
                .membershipGroup(request.membershipGroup())
                .system(false)
                .build();

        RoleEntity saved = roleEntityRepository.save(role);

        // includedRoleKeys 처리
        List<RoleInclude> includes = new ArrayList<>();
        if (request.includedRoleKeys() != null) {
            for (String includedKey : request.includedRoleKeys()) {
                RoleEntity includedRole = roleEntityRepository.findByRoleKey(includedKey)
                        .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
                includes.add(roleIncludeRepository.save(new RoleInclude(saved, includedRole)));
            }
        }

        logAudit(AuditEventType.ROLE_ASSIGNED, createdBy, null, "Role created: " + request.roleKey());
        log.info("Role created: roleKey={}, by={}", request.roleKey(), createdBy);
        return RoleResponse.from(saved, includes);
    }

    /**
     * 역할의 displayName과 description을 수정합니다.
     */
    @Transactional
    public RoleResponse updateRole(String roleKey, UpdateRoleRequest request, String updatedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        role.update(request.displayName(), request.description());
        List<RoleInclude> includes = roleIncludeRepository.findByRole(role);
        log.info("Role updated: roleKey={}, by={}", roleKey, updatedBy);
        return RoleResponse.from(role, includes);
    }

    /**
     * 역할의 활성/비활성 상태를 토글합니다.
     */
    @Transactional
    public RoleResponse toggleRoleActive(String roleKey, boolean active, String updatedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        if (active) {
            role.activate();
        } else {
            role.deactivate();
        }
        List<RoleInclude> includes = roleIncludeRepository.findByRole(role);
        log.info("Role status changed: roleKey={}, active={}, by={}", roleKey, active, updatedBy);
        return RoleResponse.from(role, includes);
    }

    /**
     * 역할에 할당된 권한 목록을 조회합니다.
     */
    public List<PermissionResponse> getRolePermissions(String roleKey) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        return rolePermissionRepository.findByRole(role).stream()
                .map(rp -> PermissionResponse.from(rp.getPermission()))
                .toList();
    }

    /**
     * 역할에 권한을 할당합니다.
     */
    @Transactional
    public void assignPermissionToRole(String roleKey, String permissionKey, String assignedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        PermissionEntity permission = permissionRepository.findByPermissionKey(permissionKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.PERMISSION_NOT_FOUND));

        if (rolePermissionRepository.existsByRoleAndPermissionId(role, permission.getId())) {
            return;
        }

        rolePermissionRepository.save(new RolePermission(role, permission));
        logAudit(AuditEventType.PERMISSION_ADDED, assignedBy, null,
                "Permission " + permissionKey + " assigned to role " + roleKey);
        log.info("Permission assigned: role={}, permission={}, by={}", roleKey, permissionKey, assignedBy);
    }

    /**
     * 역할에서 권한을 해제합니다.
     */
    @Transactional
    public void removePermissionFromRole(String roleKey, String permissionKey, String removedBy) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        PermissionEntity permission = permissionRepository.findByPermissionKey(permissionKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.PERMISSION_NOT_FOUND));

        rolePermissionRepository.deleteByRoleAndPermissionId(role, permission.getId());
        logAudit(AuditEventType.PERMISSION_REMOVED, removedBy, null,
                "Permission " + permissionKey + " removed from role " + roleKey);
        log.info("Permission removed: role={}, permission={}, by={}", roleKey, permissionKey, removedBy);
    }

    /**
     * 역할에 include를 추가합니다 (자기참조, 중복, cycle 검사 포함).
     */
    @Transactional
    public void addRoleInclude(String roleKey, String includedRoleKey, String adminId) {
        if (roleKey.equals(includedRoleKey)) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_INCLUDE_SELF_REFERENCE);
        }

        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        RoleEntity includedRole = roleEntityRepository.findByRoleKey(includedRoleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        if (roleIncludeRepository.existsByRoleAndIncludedRole(role, includedRole)) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_INCLUDE_ALREADY_EXISTS);
        }

        if (roleHierarchyService.wouldCreateCycle(roleKey, includedRoleKey)) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_INCLUDE_CYCLE_DETECTED);
        }

        roleIncludeRepository.save(new RoleInclude(role, includedRole));
        logAudit(AuditEventType.ROLE_ASSIGNED, adminId, null,
                "Role include added: " + roleKey + " → " + includedRoleKey);
        log.info("Role include added: {} → {}, by={}", roleKey, includedRoleKey, adminId);
    }

    /**
     * 역할에서 include를 제거합니다.
     */
    @Transactional
    public void removeRoleInclude(String roleKey, String includedRoleKey, String adminId) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        RoleEntity includedRole = roleEntityRepository.findByRoleKey(includedRoleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        if (!roleIncludeRepository.existsByRoleAndIncludedRole(role, includedRole)) {
            throw new CustomBusinessException(AuthErrorCode.ROLE_INCLUDE_NOT_FOUND);
        }

        roleIncludeRepository.deleteByRoleAndIncludedRole(role, includedRole);
        logAudit(AuditEventType.ROLE_REVOKED, adminId, null,
                "Role include removed: " + roleKey + " → " + includedRoleKey);
        log.info("Role include removed: {} → {}, by={}", roleKey, includedRoleKey, adminId);
    }

    /**
     * 역할의 direct includes 목록을 조회합니다.
     */
    public List<RoleResponse> getRoleIncludes(String roleKey) {
        RoleEntity role = roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));
        List<RoleInclude> includes = roleIncludeRepository.findByRole(role);
        return includes.stream()
                .map(ri -> {
                    RoleEntity included = ri.getIncludedRole();
                    List<RoleInclude> subIncludes = roleIncludeRepository.findByRole(included);
                    return RoleResponse.from(included, subIncludes);
                })
                .toList();
    }

    /**
     * 역할의 effective roles와 effective permissions를 해결합니다.
     */
    public ResolvedRoleResponse getResolvedRole(String roleKey) {
        roleEntityRepository.findByRoleKey(roleKey)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.ROLE_NOT_FOUND));

        List<String> effectiveRoles = roleHierarchyService.resolveEffectiveRoles(List.of(roleKey));
        List<String> effectivePermissions = effectiveRoles.isEmpty()
                ? List.of()
                : rolePermissionRepository.findPermissionKeysByRoleKeys(effectiveRoles).stream()
                        .distinct()
                        .toList();

        return new ResolvedRoleResponse(roleKey, effectiveRoles, effectivePermissions);
    }

    /**
     * 전체 역할 계층 DAG 구조를 반환합니다.
     */
    public RoleHierarchyResponse getRoleHierarchy() {
        return new RoleHierarchyResponse(roleHierarchyService.getHierarchyGraph());
    }

    /**
     * 모든 활성 권한 목록을 조회합니다.
     */
    public List<PermissionResponse> getAllActivePermissions() {
        return permissionRepository.findByActiveTrue().stream()
                .map(PermissionResponse::from)
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
            String membershipGroup = membership.getMembershipGroup();
            String tierKey = membership.getTier().getTierKey();
            memberships.put(membershipGroup, tierKey);

            List<String> tierPermissions = membershipTierPermissionRepository
                    .findPermissionKeysByGroupAndTier(membershipGroup, tierKey);
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

        // 역할 할당 이벤트 발행 → 멤버십 자동 할당 + Kafka 발행
        eventPublisher.publishEvent(RoleAssignedEvent.newBuilder()
                .setUserId(request.userId()).setRoleKey(request.roleKey()).setAssignedBy(assignedBy)
                .setTimestamp(java.time.Instant.now()).build());

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

    /**
     * 대시보드 통계를 조합하여 반환합니다.
     */
    public DashboardStatsResponse getDashboardStats() {
        // 1. User stats
        long totalUsers = userRepository.count();
        Map<String, Long> userByStatus = new LinkedHashMap<>();
        for (UserStatus status : UserStatus.values()) {
            userByStatus.put(status.name(), userRepository.countByStatus(status));
        }
        var userStats = new DashboardStatsResponse.UserStats(totalUsers, userByStatus);

        // 2. Role stats
        List<RoleEntity> activeRoles = roleEntityRepository.findByActiveTrue();
        int systemCount = (int) activeRoles.stream().filter(RoleEntity::isSystem).count();

        Map<String, Long> roleCounts = new HashMap<>();
        userRoleRepository.countGroupByRoleKey().forEach(row ->
                roleCounts.put((String) row[0], (Long) row[1])
        );

        List<DashboardStatsResponse.RoleAssignmentCount> assignments = activeRoles.stream()
                .map(r -> new DashboardStatsResponse.RoleAssignmentCount(
                        r.getRoleKey(), r.getDisplayName(),
                        roleCounts.getOrDefault(r.getRoleKey(), 0L)
                ))
                .toList();

        var roleStats = new DashboardStatsResponse.RoleStats(activeRoles.size(), systemCount, assignments);

        // 3. Membership stats
        Map<String, List<MembershipTier>> tiersByGroup = membershipTierRepository.findByActiveTrue().stream()
                .collect(Collectors.groupingBy(MembershipTier::getMembershipGroup));

        Map<String, Map<String, Long>> membershipCounts = new HashMap<>();
        userMembershipRepository.countActiveGroupByGroupAndTier().forEach(row -> {
            String group = (String) row[0];
            String tierKey = (String) row[1];
            Long count = (Long) row[3];
            membershipCounts.computeIfAbsent(group, k -> new HashMap<>()).put(tierKey, count);
        });

        List<String> knownGroups = List.of(
                MembershipGroupConstants.USER_SHOPPING,
                MembershipGroupConstants.USER_BLOG,
                MembershipGroupConstants.SELLER_SHOPPING
        );

        List<DashboardStatsResponse.GroupStats> groupStatsList = knownGroups.stream().map(group -> {
            Map<String, Long> tierCountMap = membershipCounts.getOrDefault(group, Map.of());
            long activeCount = tierCountMap.values().stream().mapToLong(Long::longValue).sum();

            List<MembershipTier> tiers = tiersByGroup.getOrDefault(group, List.of());
            List<DashboardStatsResponse.TierCount> tierCountList = tiers.stream()
                    .map(t -> new DashboardStatsResponse.TierCount(
                            t.getTierKey(), t.getDisplayName(),
                            tierCountMap.getOrDefault(t.getTierKey(), 0L)
                    ))
                    .toList();

            return new DashboardStatsResponse.GroupStats(group, activeCount, tierCountList);
        }).toList();

        var membershipStats = new DashboardStatsResponse.MembershipStats(groupStatsList);

        // 4. Seller stats
        long pending = sellerApplicationRepository.countByStatus(SellerApplicationStatus.PENDING);
        long approved = sellerApplicationRepository.countByStatus(SellerApplicationStatus.APPROVED);
        long rejected = sellerApplicationRepository.countByStatus(SellerApplicationStatus.REJECTED);
        var sellerStats = new DashboardStatsResponse.SellerStats(pending, approved, rejected);

        // 5. Recent activity (최근 5건)
        List<AuthAuditLog> recentLogs = auditLogRepository.findTop5ByOrderByCreatedAtDesc();
        List<DashboardStatsResponse.RecentActivityItem> recentActivity = recentLogs.stream()
                .map(l -> new DashboardStatsResponse.RecentActivityItem(
                        l.getEventType().name(),
                        l.getTargetUserId(),
                        l.getActorUserId(),
                        l.getDetails(),
                        l.getCreatedAt() != null
                                ? l.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
                ))
                .toList();

        return new DashboardStatsResponse(userStats, roleStats, membershipStats, sellerStats, recentActivity);
    }

    /**
     * 전체 감사 로그를 페이징 조회합니다.
     */
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::from);
    }

    /**
     * 특정 사용자의 감사 로그를 페이징 조회합니다.
     */
    public Page<AuditLogResponse> getUserAuditLogs(String userId, Pageable pageable) {
        return auditLogRepository.findByTargetUserId(userId, pageable)
                .map(AuditLogResponse::from);
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
