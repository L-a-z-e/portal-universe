package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacInitializationService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;

    @Transactional
    public void initializeNewUser(String userId) {
        if (!userRoleRepository.findByUserId(userId).isEmpty()) {
            log.debug("RBAC already initialized for user: {}", userId);
            return;
        }

        assignDefaultRole(userId);
        createDefaultMemberships(userId);

        log.info("RBAC initialized for new user: {}", userId);
    }

    private void assignDefaultRole(String userId) {
        RoleEntity userRole = roleEntityRepository.findByRoleKey("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Required role ROLE_USER not found in database. Ensure RBAC data is properly initialized."));
        userRoleRepository.save(UserRole.builder()
                .userId(userId)
                .role(userRole)
                .assignedBy("SYSTEM_REGISTRATION")
                .build());
    }

    private void createDefaultMemberships(String userId) {
        createMembershipIfAbsent(userId, MembershipGroupConstants.USER_BLOG);
        createMembershipIfAbsent(userId, MembershipGroupConstants.USER_SHOPPING);
    }

    private void createMembershipIfAbsent(String userId, String membershipGroup) {
        if (userMembershipRepository.existsByUserIdAndMembershipGroup(userId, membershipGroup)) {
            return;
        }

        MembershipTier freeTier = membershipTierRepository.findByMembershipGroupAndTierKey(membershipGroup, "FREE")
                .orElseThrow(() -> new IllegalStateException(
                        "Required membership tier FREE not found for group: " + membershipGroup
                                + ". Ensure RBAC data is properly initialized."));
        userMembershipRepository.save(UserMembership.builder()
                .userId(userId)
                .membershipGroup(membershipGroup)
                .tier(freeTier)
                .build());
    }
}
