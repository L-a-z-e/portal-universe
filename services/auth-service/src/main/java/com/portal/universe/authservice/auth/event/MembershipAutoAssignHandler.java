package com.portal.universe.authservice.auth.event;

import com.portal.universe.authservice.auth.domain.*;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.RoleDefaultMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.event.auth.RoleAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 역할 할당 시 role_default_memberships 테이블 기반으로
 * 멤버십을 자동 생성하는 동기 이벤트 핸들러.
 *
 * @EventListener: 호출자 트랜잭션에 동기적으로 참여하여
 * 멤버십 생성이 역할 할당과 같은 트랜잭션에서 원자적으로 커밋됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipAutoAssignHandler {

    private final RoleDefaultMembershipRepository roleDefaultMembershipRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(RoleAssignedEvent event) {
        List<RoleDefaultMembership> mappings = roleDefaultMembershipRepository.findByRoleKey(event.roleKey());

        if (mappings.isEmpty()) {
            log.debug("No default membership mappings for role: {}", event.roleKey());
            return;
        }

        for (RoleDefaultMembership mapping : mappings) {
            String group = mapping.getMembershipGroup();
            String tierKey = mapping.getDefaultTierKey();

            // 이미 해당 그룹의 멤버십이 존재하면 skip
            if (userMembershipRepository.existsByUserIdAndMembershipGroup(event.userId(), group)) {
                log.debug("Membership already exists: userId={}, group={}", event.userId(), group);
                continue;
            }

            // Tier 조회 및 활성 여부 확인
            MembershipTier tier = membershipTierRepository
                    .findByMembershipGroupAndTierKey(group, tierKey)
                    .orElse(null);

            if (tier == null) {
                log.warn("Default tier not found: group={}, tierKey={}", group, tierKey);
                continue;
            }

            if (!tier.isActive()) {
                log.warn("Default tier is inactive, skipping: group={}, tierKey={}", group, tierKey);
                continue;
            }

            userMembershipRepository.save(UserMembership.builder()
                    .userId(event.userId())
                    .membershipGroup(group)
                    .tier(tier)
                    .build());

            log.info("Auto-assigned membership: userId={}, group={}, tier={}",
                    event.userId(), group, tierKey);
        }
    }
}
