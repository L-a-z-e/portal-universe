package com.portal.universe.authservice.auth.repository;

import com.portal.universe.authservice.LocalIntegrationTest;
import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.UserMembership;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserMembershipRepository 통합 테스트
 * Phase 4-4: JPQL active tier 필터 검증
 * Testcontainers MySQL 환경에서 실행됩니다.
 */
@Transactional
class UserMembershipRepositoryIntegrationTest extends LocalIntegrationTest {

    @Autowired
    private UserMembershipRepository userMembershipRepository;

    @Autowired
    private MembershipTierRepository membershipTierRepository;

    @Autowired
    private EntityManager entityManager;

    private String testUserId;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        uniqueSuffix = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    @Nested
    @DisplayName("findActiveByUserId - Phase 4-4 JPQL 필터 검증")
    class FindActiveByUserId {

        @Test
        @DisplayName("should_returnActiveMembership_when_tierIsActive")
        void should_returnActiveMembership_when_tierIsActive() {
            String svc = "test:service" + uniqueSuffix;
            String tierKey = "TIER_A_" + uniqueSuffix;

            MembershipTier activeTier = MembershipTier.builder()
                    .membershipGroup(svc)
                    .tierKey(tierKey)
                    .displayName("테스트 티어")
                    .priceMonthly(BigDecimal.valueOf(9900))
                    .sortOrder(1)
                    .build();
            activeTier = membershipTierRepository.save(activeTier);

            UserMembership membership = UserMembership.builder()
                    .userId(testUserId)
                    .membershipGroup(svc)
                    .tier(activeTier)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .autoRenew(true)
                    .build();
            userMembershipRepository.save(membership);

            entityManager.flush();
            entityManager.clear();

            List<UserMembership> result = userMembershipRepository.findActiveByUserId(testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMembershipGroup()).isEqualTo(svc);
            assertThat(result.get(0).getTier().getTierKey()).isEqualTo(tierKey);
        }

        @Test
        @DisplayName("should_excludeMembership_when_tierIsInactive - Phase 4-4 핵심 검증")
        void should_excludeMembership_when_tierIsInactive() {
            String svc = "test:inactive" + uniqueSuffix;
            String tierKey = "TIER_INACT_" + uniqueSuffix;

            MembershipTier inactiveTier = MembershipTier.builder()
                    .membershipGroup(svc)
                    .tierKey(tierKey)
                    .displayName("비활성 티어")
                    .priceMonthly(BigDecimal.valueOf(4900))
                    .sortOrder(1)
                    .build();
            inactiveTier = membershipTierRepository.save(inactiveTier);

            entityManager.createNativeQuery("UPDATE membership_tiers SET is_active = false WHERE id = :id")
                    .setParameter("id", inactiveTier.getId())
                    .executeUpdate();

            UserMembership membership = UserMembership.builder()
                    .userId(testUserId)
                    .membershipGroup(svc)
                    .tier(inactiveTier)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .autoRenew(false)
                    .build();
            userMembershipRepository.save(membership);

            entityManager.flush();
            entityManager.clear();

            List<UserMembership> result = userMembershipRepository.findActiveByUserId(testUserId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_excludeMembership_when_statusIsNotActive")
        void should_excludeMembership_when_statusIsNotActive() {
            String svc = "test:cancel" + uniqueSuffix;
            String tierKey = "TIER_CANCEL_" + uniqueSuffix;

            MembershipTier activeTier = MembershipTier.builder()
                    .membershipGroup(svc)
                    .tierKey(tierKey)
                    .displayName("취소 테스트 티어")
                    .priceMonthly(BigDecimal.valueOf(0))
                    .sortOrder(0)
                    .build();
            activeTier = membershipTierRepository.save(activeTier);

            UserMembership membership = UserMembership.builder()
                    .userId(testUserId)
                    .membershipGroup(svc)
                    .tier(activeTier)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .autoRenew(false)
                    .build();
            membership = userMembershipRepository.save(membership);

            // membership을 CANCELLED로 변경
            membership.cancel();
            userMembershipRepository.save(membership);

            entityManager.flush();
            entityManager.clear();

            List<UserMembership> result = userMembershipRepository.findActiveByUserId(testUserId);

            // status가 CANCELLED이므로 결과에 포함되지 않아야 함
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_returnOnlyActiveWithActiveTier_when_mixedData")
        void should_returnOnlyActiveWithActiveTier_when_mixedData() {
            String svcActive = "test:active" + uniqueSuffix;
            String svcInactive = "test:inactive2" + uniqueSuffix;

            MembershipTier activeTier = MembershipTier.builder()
                    .membershipGroup(svcActive)
                    .tierKey("TIER_GOLD_" + uniqueSuffix)
                    .displayName("골드")
                    .priceMonthly(BigDecimal.valueOf(19900))
                    .sortOrder(2)
                    .build();
            activeTier = membershipTierRepository.save(activeTier);

            MembershipTier inactiveTier = MembershipTier.builder()
                    .membershipGroup(svcInactive)
                    .tierKey("TIER_SILVER_" + uniqueSuffix)
                    .displayName("실버")
                    .priceMonthly(BigDecimal.valueOf(9900))
                    .sortOrder(1)
                    .build();
            inactiveTier = membershipTierRepository.save(inactiveTier);

            entityManager.createNativeQuery("UPDATE membership_tiers SET is_active = false WHERE id = :id")
                    .setParameter("id", inactiveTier.getId())
                    .executeUpdate();

            UserMembership activeMembership = UserMembership.builder()
                    .userId(testUserId)
                    .membershipGroup(svcActive)
                    .tier(activeTier)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .autoRenew(true)
                    .build();
            userMembershipRepository.save(activeMembership);

            UserMembership inactiveTierMembership = UserMembership.builder()
                    .userId(testUserId)
                    .membershipGroup(svcInactive)
                    .tier(inactiveTier)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .autoRenew(false)
                    .build();
            userMembershipRepository.save(inactiveTierMembership);

            entityManager.flush();
            entityManager.clear();

            List<UserMembership> result = userMembershipRepository.findActiveByUserId(testUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMembershipGroup()).isEqualTo(svcActive);
        }
    }
}
