package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.LocalIntegrationTest;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.authservice.auth.domain.RoleEntity;
import com.portal.universe.authservice.auth.domain.UserRole;
import com.portal.universe.authservice.auth.repository.RoleEntityRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TokenService 통합 테스트
 * Testcontainers MySQL + Redis 환경에서 실행됩니다.
 */
@Transactional
class TokenServiceIntegrationTest extends LocalIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new User("tokentest@example.com", "encoded_password");
        testUser = userRepository.save(testUser);

        // 프로필 생성
        UserProfile profile = new UserProfile(testUser, "TestNick", "Test User", false);
        testUser.setProfile(profile);
        testUser = userRepository.save(testUser);

        // RBAC 역할 할당
        RoleEntity userRole = roleEntityRepository.findByRoleKey("ROLE_USER")
                .orElseGet(() -> {
                    RoleEntity role = RoleEntity.builder()
                            .roleKey("ROLE_USER")
                            .displayName("일반 사용자")
                            .description("기본 사용자 역할")
                            .system(true)
                            .build();
                    return roleEntityRepository.save(role);
                });

        UserRole assignment = UserRole.builder()
                .userId(testUser.getUuid())
                .role(userRole)
                .build();
        userRoleRepository.save(assignment);
    }

    @Nested
    @DisplayName("Access Token 생성")
    class GenerateAccessToken {

        @Test
        @DisplayName("should_generateValidToken_when_userHasRoles")
        void should_generateValidToken_when_userHasRoles() {
            String token = tokenService.generateAccessToken(testUser);

            assertThat(token).isNotNull().isNotBlank();

            Claims claims = tokenService.validateAccessToken(token);
            assertThat(claims.getSubject()).isEqualTo(testUser.getUuid());
            assertThat(claims.get("email")).isEqualTo("tokentest@example.com");
            assertThat(claims.get("nickname")).isEqualTo("TestNick");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            assertThat(roles).contains("ROLE_USER");
        }

        @Test
        @DisplayName("should_throwException_when_userHasNoRoles - Phase 1-3 RBAC orElseThrow 검증")
        void should_throwException_when_userHasNoRoles() {
            // RBAC 역할이 없는 사용자
            User noRoleUser = new User("norole@example.com", "encoded_password");
            noRoleUser = userRepository.save(noRoleUser);

            User finalNoRoleUser = noRoleUser;
            assertThatThrownBy(() -> tokenService.generateAccessToken(finalNoRoleUser))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No roles assigned to user");
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTest {

        @Test
        @DisplayName("should_generateAndValidateRefreshToken")
        void should_generateAndValidateRefreshToken() {
            String refreshToken = tokenService.generateRefreshToken(testUser);

            assertThat(refreshToken).isNotNull().isNotBlank();

            Claims claims = tokenService.validateRefreshToken(refreshToken);
            assertThat(claims.getSubject()).isEqualTo(testUser.getUuid());
        }
    }

    @Nested
    @DisplayName("parseClaimsAllowExpired - Phase 1-2 만료 토큰 로그아웃 검증")
    class ParseClaimsAllowExpired {

        @Test
        @DisplayName("should_returnClaims_when_tokenIsValid")
        void should_returnClaims_when_tokenIsValid() {
            String token = tokenService.generateAccessToken(testUser);

            Claims claims = tokenService.parseClaimsAllowExpired(token);

            assertThat(claims.getSubject()).isEqualTo(testUser.getUuid());
        }
    }

    @Nested
    @DisplayName("getRemainingExpiration")
    class GetRemainingExpirationTest {

        @Test
        @DisplayName("should_returnPositiveValue_when_tokenIsNotExpired")
        void should_returnPositiveValue_when_tokenIsNotExpired() {
            String token = tokenService.generateAccessToken(testUser);

            long remaining = tokenService.getRemainingExpiration(token);

            assertThat(remaining).isGreaterThan(0);
            assertThat(remaining).isLessThanOrEqualTo(900000L);
        }
    }
}
