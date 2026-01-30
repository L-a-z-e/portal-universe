package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 사용자 등록 시 RBAC 데이터를 즉시 초기화합니다.
 * ROLE_USER 할당 + shopping/blog FREE 멤버십 생성.
 * 이미 초기화된 사용자는 스킵합니다 (멱등성 보장).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacInitializationService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;

    /**
     * 신규 사용자의 RBAC 데이터를 초기화합니다.
     * - ROLE_USER 할당
     * - shopping FREE, blog FREE 멤버십 생성
     *
     * @param userId 사용자 UUID
     */
    @Transactional
    public void initializeNewUser(String userId) {
        // 이미 초기화된 사용자는 스킵
        if (!userRoleRepository.findByUserId(userId).isEmpty()) {
            log.debug("RBAC already initialized for user: {}", userId);
            return;
        }

        assignDefaultRole(userId);
        createDefaultMemberships(userId);

        log.info("RBAC initialized for new user: {}", userId);
    }

    private void assignDefaultRole(String userId) {
        roleEntityRepository.findByRoleKey("ROLE_USER").ifPresent(userRole ->
                userRoleRepository.save(UserRole.builder()
                        .userId(userId)
                        .role(userRole)
                        .assignedBy("SYSTEM_REGISTRATION")
                        .build())
        );
    }

    private void createDefaultMemberships(String userId) {
        createMembershipIfAbsent(userId, "shopping");
        createMembershipIfAbsent(userId, "blog");
    }

    private void createMembershipIfAbsent(String userId, String serviceName) {
        if (userMembershipRepository.existsByUserIdAndServiceName(userId, serviceName)) {
            return;
        }

        membershipTierRepository.findByServiceNameAndTierKey(serviceName, "FREE")
                .ifPresent(freeTier ->
                        userMembershipRepository.save(UserMembership.builder()
                                .userId(userId)
                                .serviceName(serviceName)
                                .tier(freeTier)
                                .build())
                );
    }
}
