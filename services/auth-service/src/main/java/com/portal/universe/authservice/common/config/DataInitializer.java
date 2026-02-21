package com.portal.universe.authservice.common.config;

import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.auth.service.RbacInitializationService;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 로컬 개발 환경에서 테스트 데이터를 자동으로 생성하는 설정 클래스입니다.
 * RbacDataMigrationRunner(@Order(1)) 이후에 실행되어야 합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RbacInitializationService rbacInitializationService;
    private final RoleEntityRepository roleEntityRepository;
    private final UserRoleRepository userRoleRepository;

    @Bean
    @Order(2)
    @Profile({"local", "docker", "kubernetes"})
    public CommandLineRunner initData() {
        return args -> {
            // 일반 테스트 유저 생성 (ROLE_USER + FREE 멤버십)
            createTestUser(
                    "test@test.com",
                    "test1234",
                    "테스트사용자",
                    "testuser",
                    "laze",
                    false,
                    "00000000-0000-0000-0000-000000000001"
            );

            // Admin 테스트 유저 생성 (ROLE_USER + ROLE_SUPER_ADMIN + FREE 멤버십)
            createTestUser(
                    "admin@test.com",
                    "admin1234",
                    "관리자",
                    "admin",
                    "laze",
                    true,
                    "00000000-0000-0000-0000-000000000002"
            );

            // 블로거 테스트 유저 5명 (ROLE_USER + FREE 멤버십)
            createTestUser(
                    "dev.kim@test.com",
                    "test1234",
                    "김개발",
                    "dev_kim",
                    "김민수",
                    false,
                    "00000000-0000-0000-0000-000000000101"
            );

            createTestUser(
                    "travel.lee@test.com",
                    "test1234",
                    "이여행",
                    "travel_lee",
                    "이수진",
                    false,
                    "00000000-0000-0000-0000-000000000102"
            );

            createTestUser(
                    "design.park@test.com",
                    "test1234",
                    "박디자인",
                    "design_park",
                    "박지현",
                    false,
                    "00000000-0000-0000-0000-000000000103"
            );

            createTestUser(
                    "study.choi@test.com",
                    "test1234",
                    "최공부",
                    "study_choi",
                    "최준호",
                    false,
                    "00000000-0000-0000-0000-000000000104"
            );

            createTestUser(
                    "cook.jung@test.com",
                    "test1234",
                    "정맛집",
                    "cook_jung",
                    "정서연",
                    false,
                    "00000000-0000-0000-0000-000000000105"
            );
        };
    }

    /**
     * 테스트 유저를 생성하는 헬퍼 메서드입니다.
     * 이미 존재하는 이메일이면 스킵합니다.
     * RBAC 초기화(ROLE_USER + FREE 멤버십)를 함께 수행합니다.
     *
     * @param isAdmin true이면 ROLE_SUPER_ADMIN 추가 할당
     * @param fixedUuid 고정 UUID (blog-service seed data 매칭용)
     */
    private void createTestUser(String email, String password, String nickname, String username,
                                String realName, boolean isAdmin, String fixedUuid) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Test user already exists: email={}", email);
            return;
        }

        log.info("Creating test user: email={}, admin={}", email, isAdmin);

        User user = new User(email, passwordEncoder.encode(password));
        user.assignUuid(fixedUuid);
        UserProfile profile = new UserProfile(user, nickname, realName, true);
        profile.setUsername(username);
        user.setProfile(profile);
        User savedUser = userRepository.save(user);

        // RBAC 초기화 (ROLE_USER + shopping/blog FREE 멤버십)
        rbacInitializationService.initializeNewUser(savedUser.getUuid());

        // 관리자인 경우 ROLE_SUPER_ADMIN 추가 할당
        if (isAdmin) {
            roleEntityRepository.findByRoleKey("ROLE_SUPER_ADMIN").ifPresent(adminRole -> {
                userRoleRepository.save(UserRole.builder()
                        .userId(savedUser.getUuid())
                        .role(adminRole)
                        .assignedBy("SYSTEM_INIT")
                        .build());
                log.info("ROLE_SUPER_ADMIN assigned to: {}", email);
            });
        }

        log.info("Test user created: email={}, uuid={}", email, savedUser.getUuid());
    }
}
