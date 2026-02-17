package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacInitializationService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void initializeNewUser(String userId) {
        if (!userRoleRepository.findByUserId(userId).isEmpty()) {
            log.debug("RBAC already initialized for user: {}", userId);
            return;
        }

        assignDefaultRole(userId);

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

        // 역할 할당 이벤트 → MembershipAutoAssignHandler가 기본 멤버십 자동 생성
        eventPublisher.publishEvent(RoleAssignedEvent.of(userId, "ROLE_USER", "SYSTEM_REGISTRATION"));
    }
}
