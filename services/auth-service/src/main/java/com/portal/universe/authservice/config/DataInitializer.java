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
            String testEmail = "test@example.com";

            if (userRepository.findByEmail(testEmail).isEmpty()) {
                log.info("Initializing test data...");

                // 1. 테스트 유저 생성
                User testUser = new User(
                        testEmail,
                        passwordEncoder.encode("password123"),
                        Role.USER
                );

                // 2. 프로필 연결
                UserProfile profile = new UserProfile(
                        testUser,
                        "테스트유저",
                        "홍길동",
                        true
                );
                testUser.setProfile(profile);

                // 3. 저장
                userRepository.save(testUser);

                log.info("Test user created: email={}, password={}", testEmail, "password123");
            } else {
                log.info("Test data already exists. Skipping initialization.");
            }
        };
    }
}
