package com.portal.universe.authservice.auth.service;

import com.portal.universe.authservice.auth.domain.MembershipTier;
import com.portal.universe.authservice.auth.domain.UserMembership;
import com.portal.universe.authservice.auth.repository.UserMembershipRepository;
import com.portal.universe.authservice.auth.repository.UserRoleRepository;
import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.domain.UserProfile;
import com.portal.universe.authservice.util.JwtTestHelper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    private static final String SECRET_KEY = JwtTestHelper.SECRET_KEY;
    private static final String KEY_ID = "test-key";
    private static final String USER_UUID = JwtTestHelper.USER_UUID;
    private static final String USER_EMAIL = JwtTestHelper.USER_EMAIL;
    private static final long ACCESS_TOKEN_EXPIRATION = 900_000L; // 15min
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L; // 7days

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserMembershipRepository userMembershipRepository;

    @InjectMocks
    private TokenService tokenService;

    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = createJwtProperties();

        // Inject real JwtProperties via reflection
        try {
            var field = TokenService.class.getDeclaredField("jwtProperties");
            field.setAccessible(true);
            field.set(tokenService, jwtProperties);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JwtProperties createJwtProperties() {
        JwtProperties props = new JwtProperties();
        props.setCurrentKeyId(KEY_ID);
        props.setAccessTokenExpiration(ACCESS_TOKEN_EXPIRATION);
        props.setRefreshTokenExpiration(REFRESH_TOKEN_EXPIRATION);

        JwtProperties.KeyConfig keyConfig = new JwtProperties.KeyConfig();
        keyConfig.setSecretKey(SECRET_KEY);
        keyConfig.setActivatedAt(LocalDateTime.now().minusDays(1));
        keyConfig.setExpiresAt(null); // never expires

        Map<String, JwtProperties.KeyConfig> keys = new HashMap<>();
        keys.put(KEY_ID, keyConfig);
        props.setKeys(keys);

        return props;
    }

    private User createTestUser() {
        User user = new User(USER_EMAIL, "encodedPassword");
        // Set uuid via reflection
        try {
            var uuidField = User.class.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uuidField.set(user, USER_UUID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        UserProfile profile = new UserProfile(user, "testNickname", "realName", false);
        user.setProfile(profile);
        return user;
    }

    private MembershipTier createFreeTier(String serviceName) {
        return MembershipTier.builder()
                .serviceName(serviceName)
                .tierKey("FREE")
                .displayName("Free")
                .sortOrder(0)
                .build();
    }

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("should_generateValidToken_when_userHasRolesAndMemberships")
        void should_generateValidToken_when_userHasRolesAndMemberships() {
            // given
            User user = createTestUser();
            when(userRoleRepository.findActiveRoleKeysByUserId(USER_UUID))
                    .thenReturn(List.of("ROLE_USER"));

            MembershipTier freeTier = createFreeTier("shopping");
            UserMembership membership = UserMembership.builder()
                    .userId(USER_UUID)
                    .serviceName("shopping")
                    .tier(freeTier)
                    .build();
            when(userMembershipRepository.findActiveByUserId(USER_UUID))
                    .thenReturn(List.of(membership));

            // when
            String token = tokenService.generateAccessToken(user);

            // then
            assertThat(token).isNotNull().isNotEmpty();

            // Verify token can be parsed
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
            assertThat(claims.get("email")).isEqualTo(USER_EMAIL);
            assertThat(claims.get("nickname")).isEqualTo("testNickname");

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            assertThat(roles).containsExactly("ROLE_USER");

            @SuppressWarnings("unchecked")
            Map<String, String> memberships = (Map<String, String>) claims.get("memberships");
            assertThat(memberships).containsEntry("shopping", "FREE");

            // Verify kid in header
            String[] parts = token.split("\\.");
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            assertThat(header).contains("\"kid\":\"" + KEY_ID + "\"");
        }

        @Test
        @DisplayName("should_throwException_when_userHasNoRoles")
        void should_throwException_when_userHasNoRoles() {
            // given
            User user = createTestUser();
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
            User user = createTestUser();

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
            String token = JwtTestHelper.createValidToken(SECRET_KEY, USER_UUID, List.of("ROLE_USER"));

            // when
            Claims claims = tokenService.validateAccessToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_throwExpiredJwtException_when_tokenExpired")
        void should_throwExpiredJwtException_when_tokenExpired() {
            // given
            String token = JwtTestHelper.createExpiredToken(SECRET_KEY, USER_UUID);

            // when & then
            assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("should_throwSignatureException_when_signatureInvalid")
        void should_throwSignatureException_when_signatureInvalid() {
            // given
            String differentSecret = "different-secret-key-must-be-at-least-256-bits!!";
            String token = JwtTestHelper.createValidToken(differentSecret, USER_UUID, List.of("ROLE_USER"));

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
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("validateTokenInternal - kid handling")
    class ValidateTokenInternalKidHandling {

        @Test
        @DisplayName("should_useCurrentKeyId_when_tokenHasNoKid")
        void should_useCurrentKeyId_when_tokenHasNoKid() {
            // given
            String token = JwtTestHelper.createTokenWithoutKid(SECRET_KEY, USER_UUID);

            // when
            Claims claims = tokenService.validateAccessToken(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_useKidFromToken_when_kidPresent")
        void should_useKidFromToken_when_kidPresent() {
            // given
            String token = JwtTestHelper.createTokenWithKid(SECRET_KEY, KEY_ID, USER_UUID);

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
            expiredKeyConfig.setActivatedAt(LocalDateTime.now().minusDays(30));
            expiredKeyConfig.setExpiresAt(LocalDateTime.now().minusDays(1)); // expired yesterday

            jwtProperties.getKeys().put("expired-key", expiredKeyConfig);

            String token = JwtTestHelper.createTokenWithKid(SECRET_KEY, "expired-key", USER_UUID);

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
            String token = JwtTestHelper.createExpiredToken(SECRET_KEY, USER_UUID);

            // when
            Claims claims = tokenService.parseClaimsAllowExpired(token);

            // then
            assertThat(claims.getSubject()).isEqualTo(USER_UUID);
        }

        @Test
        @DisplayName("should_throwSignatureException_when_signatureInvalid")
        void should_throwSignatureException_when_signatureInvalid() {
            // given
            String differentSecret = "different-secret-key-must-be-at-least-256-bits!!";
            String token = JwtTestHelper.createExpiredToken(differentSecret, USER_UUID);

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
            String token = JwtTestHelper.createValidToken(SECRET_KEY, USER_UUID, List.of("ROLE_USER"));

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
            String token = JwtTestHelper.createValidToken(SECRET_KEY, USER_UUID, List.of("ROLE_USER"));

            // when
            long remaining = tokenService.getRemainingExpiration(token);

            // then
            assertThat(remaining).isGreaterThan(0);
        }

        @Test
        @DisplayName("should_returnZero_when_tokenExpired")
        void should_returnZero_when_tokenExpired() {
            // given
            String token = JwtTestHelper.createExpiredToken(SECRET_KEY, USER_UUID);

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
