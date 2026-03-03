package com.portal.universe.authservice.common.config;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.RoleDefaultMembership;
import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserMembership;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.MembershipTierRepository;
import com.portal.universe.authservice.auth.repository.RoleDefaultMembershipRepository;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 부하 테스트용 Bulk 계정을 생성하는 설정 클래스입니다.
 * DataInitializer(@Order(2)) 이후에 실행됩니다.
 *
 * 최적화:
 * - BCrypt: 1회만 인코딩, 결과 재사용 (10,000 x ~100ms → ~0.1초)
 * - RBAC bypass: 이벤트 발행 없이 직접 생성 (MembershipAutoAssignHandler 회피)
 * - 배치 flush: 500건마다 flush + clear (메모리 관리)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkTestAccountInitializer {

    private static final int TOTAL_ACCOUNTS = 10_000;
    private static final int BATCH_SIZE = 500;
    private static final String EMAIL_PATTERN = "testaccount%04d@test.com";
    private static final String PASSWORD = "test1234";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleDefaultMembershipRepository roleDefaultMembershipRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Bean
    @Order(3)
    @Profile({"local", "docker", "kubernetes"})
    public CommandLineRunner initBulkTestAccounts() {
        return args -> {
            String firstEmail = String.format(EMAIL_PATTERN, 0);
            if (userRepository.findByEmail(firstEmail).isPresent()) {
                log.info("Bulk test accounts already exist, skipping creation");
                return;
            }

            log.info("Creating {} bulk test accounts...", TOTAL_ACCOUNTS);
            long startTime = System.currentTimeMillis();

            String encodedPassword = passwordEncoder.encode(PASSWORD);

            RoleEntity roleUser = roleEntityRepository.findByRoleKey("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

            List<RoleDefaultMembership> defaultMemberships =
                    roleDefaultMembershipRepository.findByRoleKey("ROLE_USER");

            Map<String, MembershipTier> tierMap = new HashMap<>();
            for (RoleDefaultMembership rdm : defaultMemberships) {
                membershipTierRepository
                        .findByMembershipGroupAndTierKey(rdm.getMembershipGroup(), rdm.getDefaultTierKey())
                        .ifPresent(tier -> tierMap.put(rdm.getMembershipGroup(), tier));
            }

            for (int batch = 0; batch < TOTAL_ACCOUNTS / BATCH_SIZE; batch++) {
                final int batchStart = batch * BATCH_SIZE;
                transactionTemplate.executeWithoutResult(status -> {
                    for (int i = batchStart; i < batchStart + BATCH_SIZE; i++) {
                        String email = String.format(EMAIL_PATTERN, i);
                        String nickname = String.format("테스터%04d", i);
                        String username = String.format("tester%04d", i);

                        User user = new User(email, encodedPassword);
                        UserProfile profile = new UserProfile(user, nickname, "부하테스터", true);
                        profile.setUsername(username);
                        user.setProfile(profile);
                        User savedUser = userRepository.save(user);

                        userRoleRepository.save(UserRole.builder()
                                .userId(savedUser.getUuid())
                                .role(roleUser)
                                .assignedBy("BULK_INIT")
                                .build());

                        for (Map.Entry<String, MembershipTier> entry : tierMap.entrySet()) {
                            userMembershipRepository.save(UserMembership.builder()
                                    .userId(savedUser.getUuid())
                                    .membershipGroup(entry.getKey())
                                    .tier(entry.getValue())
                                    .autoRenew(false)
                                    .build());
                        }
                    }

                    entityManager.flush();
                    entityManager.clear();
                });

                if ((batchStart + BATCH_SIZE) % 2000 == 0) {
                    log.info("Bulk test accounts progress: {}/{}", batchStart + BATCH_SIZE, TOTAL_ACCOUNTS);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Bulk test account creation completed: {} accounts in {}ms", TOTAL_ACCOUNTS, elapsed);
        };
    }
}
