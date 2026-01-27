package com.portal.universe.authservice.config;

import com.portal.universe.authservice.domain.Role;
import com.portal.universe.authservice.domain.User;
import com.portal.universe.authservice.domain.UserProfile;
import com.portal.universe.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 로컬 개발 환경에서 테스트 데이터를 자동으로 생성하는 설정 클래스입니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile({"default", "dev", "docker"}) // 특정 프로필에서만 작동하도록 설정
    public CommandLineRunner initData() {
        return args -> {
            // 일반 테스트 유저 생성
            createTestUser(
                    "test@example.com",
                    "password123",
                    Role.USER,
                    "테스트유저",
                    "홍길동"
            );

            // Admin 테스트 유저 생성
            createTestUser(
                    "admin@example.com",
                    "admin123",
                    Role.ADMIN,
                    "관리자",
                    "김관리"
            );
        };
    }

    /**
     * 테스트 유저를 생성하는 헬퍼 메서드입니다.
     * 이미 존재하는 이메일이면 스킵합니다.
     */
    private void createTestUser(String email, String password, Role role, String nickname, String realName) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("Creating test user: email={}, role={}", email, role);

            User user = new User(
                    email,
                    passwordEncoder.encode(password),
                    role
            );

            UserProfile profile = new UserProfile(
                    user,
                    nickname,
                    realName,
                    true
            );
            user.setProfile(profile);

            userRepository.save(user);

            log.info("Test user created: email={}, role={}", email, role);
        } else {
            log.info("Test user already exists: email={}", email);
        }
    }
}
