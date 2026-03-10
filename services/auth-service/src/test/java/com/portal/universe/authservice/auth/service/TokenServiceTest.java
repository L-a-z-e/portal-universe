package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.UserMembership;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.support.fixture.JwtFixture;
import com.portal.universe.authservice.support.fixture.MembershipFixture;
import com.portal.universe.authservice.support.fixture.UserFixture;
import com.portal.universe.authservice.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    private static final String SECRET_KEY = JwtFixture.SECRET_KEY;
    private static final String KEY_ID = JwtFixture.KEY_ID;
    private static final String USER_UUID = JwtFixture.USER_UUID;
    private static final String USER_EMAIL = JwtFixture.USER_EMAIL;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @Mock
    private RoleHierarchyService roleHierarchyService;

    @InjectMocks
    private TokenService tokenService;

    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = JwtFixture.createJwtProperties();
        ReflectionTestUtils.setField(tokenService, "jwtProperties", jwtProperties);
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should_generateValidToken_when_userHasRolesAndMemberships")
        void should_generateValidToken_when_userHasRolesAndMemberships() {
            // given
            User user = UserFixture.create();
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_UUID))
                    .thenReturn(List.of("ROLE_USER"));
            when(roleHierarchyService.resolveEffectiveRoles(List.of("ROLE_USER")))
                    .thenReturn(List.of("ROLE_USER", "ROLE_GUEST"));

            UserMembership membership = MembershipFixture.membershipBuilder()
                    .userId(USER_UUID)
                    .membershipGroup("user:shopping")
                    .tier(MembershipFixture.tierBuilder().membershipGroup("user:shopping").build())
                    .build();
            when(userMembershipRepository.findActiveByUserId(USER_UUID))
                    .thenReturn(List.of(membership));

            // when
            String token = tokenService.generateAccessToken(user);

            // then
            assertThat(token).isNotNull().isNotEmpty();

            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
            assertThat(claims.get("email")).isEqualTo(USER_EMAIL);
            assertThat(claims.get("nickname")).isEqualTo(UserFixture.DEFAULT_NICKNAME);

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            assertThat(roles).containsExactly("ROLE_USER");

            @SuppressWarnings("unchecked")
            List<String> effectiveRoles = (List<String>) claims.get("effectiveRoles");
            assertThat(effectiveRoles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_GUEST");

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> memberships = (Map<String, Map<String, Object>>) claims.get("memberships");
            assertThat(memberships).containsKey("user:shopping");
            assertThat(memberships.get("user:shopping").get("tier")).isEqualTo("FREE");

            // Verify kid in header
            String[] parts = token.split("\\.");
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            assertThat(header).contains("\"kid\":\"" + KEY_ID + "\"");
        }

        @Test
        @DisplayName("should_throwException_when_userHasNoRoles")
        void should_throwException_when_userHasNoRoles() {
            // given
            User user = UserFixture.create();
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_UUID))
                    .thenReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> tokenService.generateAccessToken(user))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No roles assigned to user");
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("should_generateValidRefreshToken_when_userProvided")
        void should_generateValidRefreshToken_when_userProvided() {
            // given
            User user = UserFixture.create();

            // when
            String token = tokenService.generateRefreshToken(user);

            // then
            assertThat(token).isNotNull().isNotEmpty();

            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
            assertThat(claims.getExpiration()).isNotNull();
            assertThat(claims.getExpiration().getTime())
                    .isGreaterThan(System.currentTimeMillis());
        }
    }

    @Nested
    @DisplayName("validateAccessToken")
    class ValidateAccessToken {

        @Test
        @DisplayName("should_returnClaims_when_tokenIsValid")
        void should_returnClaims_when_tokenIsValid() {
            // given
            String token = JwtFixture.createValidToken();

            // when
            Claims claims = tokenService.validateAccessToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_throwExpiredJwtException_when_tokenExpired")
        void should_throwExpiredJwtException_when_tokenExpired() {
            // given
            String token = JwtFixture.createExpiredToken();

            // when & then
            assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("should_throwSignatureException_when_signatureInvalid")
        void should_throwSignatureException_when_signatureInvalid() {
            // given
            String token = JwtFixture.tokenBuilder()
                    .secretKey("different-secret-key-must-be-at-least-256-bits!!")
                    .build();

            // when & then
            assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("should_throwMalformedJwtException_when_tokenMalformed")
        void should_throwMalformedJwtException_when_tokenMalformed() {
            // given
            String malformedToken = "not.a.valid.jwt.token";

            // when & then
            assertThatThrownBy(() -> tokenService.validateAccessToken(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }
    }

    @Nested
    @DisplayName("validateTokenInternal - kid handling")
    class ValidateTokenInternalKidHandling {

        @Test
        @DisplayName("should_useCurrentKeyId_when_tokenHasNoKid")
        void should_useCurrentKeyId_when_tokenHasNoKid() {
            // given
            String token = JwtFixture.createTokenWithoutKid();

            // when
            Claims claims = tokenService.validateAccessToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_useKidFromToken_when_kidPresent")
        void should_useKidFromToken_when_kidPresent() {
            // given
            String token = JwtFixture.createTokenWithKid(KEY_ID);

            // when
            Claims claims = tokenService.validateAccessToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_throwException_when_keyExpired")
        void should_throwException_when_keyExpired() {
            // given - add an expired key
            JwtProperties.KeyConfig expiredKeyConfig = new JwtProperties.KeyConfig();
            expiredKeyConfig.setSecretKey(SECRET_KEY);
            expiredKeyConfig.setActivatedAt(Instant.now().minus(Duration.ofDays(30)));
            expiredKeyConfig.setExpiresAt(Instant.now().minus(Duration.ofDays(1)));

            jwtProperties.getKeys().put("expired-key", expiredKeyConfig);

            String token = JwtFixture.createTokenWithKid("expired-key");

            // when & then
            assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("parseClaimsAllowExpired")
    class ParseClaimsAllowExpired {

        @Test
        @DisplayName("should_returnClaims_when_tokenExpired")
        void should_returnClaims_when_tokenExpired() {
            // given
            String token = JwtFixture.createExpiredToken();

            // when
            Claims claims = tokenService.parseClaimsAllowExpired(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_throwSignatureException_when_signatureInvalid")
        void should_throwSignatureException_when_signatureInvalid() {
            // given
            String token = JwtFixture.tokenBuilder()
                    .secretKey("different-secret-key-must-be-at-least-256-bits!!")
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .build();

            // when & then
            assertThatThrownBy(() -> tokenService.parseClaimsAllowExpired(token))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("should_returnUserId_when_tokenValid")
        void should_returnUserId_when_tokenValid() {
            // given
            String token = JwtFixture.createValidToken();

            // when
            String userId = tokenService.getUserIdFromToken(token);

            // then
            assertThat(userId).isEqualTo(USER_UUID);
        }
    }

    @Nested
    @DisplayName("getRemainingExpiration")
    class GetRemainingExpiration {

        @Test
        @DisplayName("should_returnPositiveValue_when_tokenNotExpired")
        void should_returnPositiveValue_when_tokenNotExpired() {
            // given
            String token = JwtFixture.createValidToken();

            // when
            long remaining = tokenService.getRemainingExpiration(token);

            // then
            assertThat(remaining).isGreaterThan(0);
        }

        @Test
        @DisplayName("should_returnZero_when_tokenExpired")
        void should_returnZero_when_tokenExpired() {
            // given
            String token = JwtFixture.createExpiredToken();

            // when
            long remaining = tokenService.getRemainingExpiration(token);

            // then
            assertThat(remaining).isZero();
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
