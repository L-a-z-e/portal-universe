package com.portal.universe.apigateway.filter;

import com.portal.universe.apigateway.config.JwtProperties;
import com.portal.universe.apigateway.config.PublicPathProperties;
import com.portal.universe.apigateway.service.RoleHierarchyResolver;
import com.portal.universe.apigateway.service.TokenBlacklistChecker;
import com.portal.universe.apigateway.util.JwtTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Test")
class JwtAuthenticationFilterTest {

    private static final String SECRET_KEY = JwtTestHelper.TEST_SECRET_KEY;
    private static final String KEY_ID = "test-key-1";

    @Mock
    private TokenBlacklistChecker tokenBlacklistChecker;

    @Mock
    private RoleHierarchyResolver roleHierarchyResolver;

    @Mock
    private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        var jwtProperties = new JwtProperties();
        jwtProperties.setCurrentKeyId(KEY_ID);

        var keyConfig = new JwtProperties.KeyConfig();
        keyConfig.setSecretKey(SECRET_KEY);
        keyConfig.setActivatedAt(LocalDateTime.now().minusDays(1));
        keyConfig.setExpiresAt(null); // 만료 없음

        var keys = new HashMap<String, JwtProperties.KeyConfig>();
        keys.put(KEY_ID, keyConfig);
        jwtProperties.setKeys(keys);

        var publicPathProperties = new PublicPathProperties();
        publicPathProperties.setSkipJwtParsing(List.of("/actuator", "/fallback"));

        filter = new JwtAuthenticationFilter(jwtProperties, publicPathProperties, tokenBlacklistChecker, roleHierarchyResolver);
    }

    private JwtAuthenticationFilter createFilterWithKeys(Map<String, JwtProperties.KeyConfig> keys, String currentKeyId) {
        var jwtProperties = new JwtProperties();
        jwtProperties.setCurrentKeyId(currentKeyId);
        jwtProperties.setKeys(keys);

        var publicPathProperties = new PublicPathProperties();
        publicPathProperties.setSkipJwtParsing(List.of("/actuator", "/fallback"));

        return new JwtAuthenticationFilter(jwtProperties, publicPathProperties, tokenBlacklistChecker, roleHierarchyResolver);
    }

    @Nested
    @DisplayName("공개 경로")
    class PublicPath {

        @Test
        @DisplayName("공개 경로는 JWT 검증 없이 통과한다")
        void should_passThrough_when_publicPath() {
            var request = MockServerHttpRequest.get("/actuator/health").build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(chain).filter(any());
            verify(tokenBlacklistChecker, never()).isBlacklisted(anyString());
        }

        @Test
        @DisplayName("공개 경로에서도 X-User-* 헤더를 제거한다")
        void should_stripXUserHeaders_when_publicPath() {
            var request = MockServerHttpRequest.get("/actuator/health")
                    .header("X-User-Id", "hacker")
                    .header("X-User-Roles", "ROLE_ADMIN")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var sanitizedRequest = captor.getValue().getRequest();
            assertThat(sanitizedRequest.getHeaders().getFirst("X-User-Id")).isNull();
            assertThat(sanitizedRequest.getHeaders().getFirst("X-User-Roles")).isNull();
        }
    }

    @Nested
    @DisplayName("토큰 없음")
    class NoToken {

        @Test
        @DisplayName("Authorization 헤더가 없으면 SecurityConfig에 위임한다")
        void should_passWithoutAuth_when_noAuthHeader() {
            var request = MockServerHttpRequest.get("/api/test").build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(chain).filter(any());
            verify(tokenBlacklistChecker, never()).isBlacklisted(anyString());
        }

        @Test
        @DisplayName("Bearer가 아닌 Authorization 헤더는 무시한다")
        void should_passWithoutAuth_when_nonBearerAuth() {
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(tokenBlacklistChecker, never()).isBlacklisted(anyString());
        }
    }

    @Nested
    @DisplayName("유효한 토큰")
    class ValidToken {

        @BeforeEach
        void setUpRoleHierarchy() {
            when(roleHierarchyResolver.resolveEffectiveRoles(any())).thenAnswer(invocation -> {
                List<String> roles = invocation.getArgument(0);
                return Mono.just(roles);
            });
        }

        @Test
        @DisplayName("유효한 토큰으로 X-User-* 헤더를 추가한다")
        void should_addUserHeaders_when_validToken() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo("user1");
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("유효한 토큰으로 SecurityContext에 Authentication을 설정한다")
        void should_setSecurityContext_when_validToken() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();
            when(chain.filter(any())).thenReturn(
                    ReactiveSecurityContextHolder.getContext()
                            .doOnNext(capturedContext::set)
                            .then()
            );

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            assertThat(capturedContext.get()).isNotNull();
            assertThat(capturedContext.get().getAuthentication()).isNotNull();
            assertThat(capturedContext.get().getAuthentication().getName()).isEqualTo("user1");
            assertThat(capturedContext.get().getAuthentication().getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("nickname이 있으면 URL인코딩하여 헤더에 추가한다")
        void should_addNickname_when_tokenHasNickname() {
            String token = JwtTestHelper.createTokenWithNickname(SECRET_KEY, "user1", "테스터", "testuser");
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Nickname")).isNotEmpty();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Name")).isNotEmpty();
        }

        @Test
        @DisplayName("nickname이 없으면 빈 문자열을 추가한다")
        void should_addEmptyNickname_when_tokenHasNoNickname() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Nickname")).isEmpty();
        }

        @Test
        @DisplayName("복수 roles를 쉼표로 구분하여 헤더에 추가한다")
        void should_parseMultipleRoles_when_tokenHasRoles() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER", "ROLE_SELLER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER,ROLE_SELLER");
        }

        @Test
        @DisplayName("memberships가 있으면 JSON 문자열로 헤더에 추가한다")
        void should_parseMemberships_when_tokenHasMemberships() {
            String token = JwtTestHelper.createTokenWithMemberships(SECRET_KEY, "user1", Map.of("shopping", "PREMIUM"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            String memberships = mutatedRequest.getHeaders().getFirst("X-User-Memberships");
            assertThat(memberships).contains("shopping");
            assertThat(memberships).contains("PREMIUM");
        }

        @Test
        @DisplayName("memberships가 없으면 빈 JSON 객체를 반환한다")
        void should_returnEmptyMemberships_when_noMemberships() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Memberships")).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("토큰 검증 실패")
    class InvalidToken {

        @Test
        @DisplayName("만료된 토큰은 401과 GW-A006 코드를 반환한다")
        void should_return401_when_tokenExpired() {
            String token = JwtTestHelper.createExpiredToken(SECRET_KEY, "user1");
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            String body = exchange.getResponse().getBodyAsString().block();
            assertThat(body).contains("GW-A006");
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("잘못된 서명의 토큰은 401과 GW-A007 코드를 반환한다")
        void should_return401_when_invalidSignature() {
            String wrongKey = "wrong-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-testing";
            String token = JwtTestHelper.createValidToken(wrongKey, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            String body = exchange.getResponse().getBodyAsString().block();
            assertThat(body).contains("GW-A007");
        }

        @Test
        @DisplayName("malformed 토큰은 401을 반환한다")
        void should_return401_when_malformedToken() {
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt.token")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("블랙리스트")
    class Blacklist {

        @Test
        @DisplayName("블랙리스트 토큰은 401과 GW-A005 코드를 반환한다")
        void should_return401_when_tokenBlacklisted() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(token)).thenReturn(Mono.just(true));

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            String body = exchange.getResponse().getBodyAsString().block();
            assertThat(body).contains("GW-A005");
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Key Rotation")
    class KeyRotation {

        @Test
        @DisplayName("kid가 있는 토큰은 해당 키로 검증한다")
        void should_validateWithKid_when_tokenHasKid() {
            when(roleHierarchyResolver.resolveEffectiveRoles(any())).thenAnswer(invocation -> {
                List<String> roles = invocation.getArgument(0);
                return Mono.just(roles);
            });
            String key2Secret = "another-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";
            var key2Config = new JwtProperties.KeyConfig();
            key2Config.setSecretKey(key2Secret);
            key2Config.setActivatedAt(LocalDateTime.now().minusDays(1));

            var keys = new HashMap<String, JwtProperties.KeyConfig>();
            var key1Config = new JwtProperties.KeyConfig();
            key1Config.setSecretKey(SECRET_KEY);
            key1Config.setActivatedAt(LocalDateTime.now().minusDays(1));
            keys.put(KEY_ID, key1Config);
            keys.put("test-key-2", key2Config);

            var testFilter = createFilterWithKeys(keys, KEY_ID);

            String token = JwtTestHelper.createTokenWithKid(key2Secret, "test-key-2", "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(testFilter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("kid가 없는 토큰은 currentKeyId로 검증한다")
        void should_useCurrentKeyId_when_noKid() {
            when(roleHierarchyResolver.resolveEffectiveRoles(any())).thenAnswer(invocation -> {
                List<String> roles = invocation.getArgument(0);
                return Mono.just(roles);
            });
            String token = JwtTestHelper.createToken(SECRET_KEY, null, "user1",
                    List.of("ROLE_USER"), null, null, null, 3600_000L);
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(chain).filter(any());
        }

        @Test
        @DisplayName("만료된 키로 서명된 토큰은 IllegalArgumentException을 발생시킨다")
        void should_throwException_when_keyExpired() {
            var expiredConfig = new JwtProperties.KeyConfig();
            expiredConfig.setSecretKey(SECRET_KEY);
            expiredConfig.setActivatedAt(LocalDateTime.now().minusDays(30));
            expiredConfig.setExpiresAt(LocalDateTime.now().minusDays(1));

            var keys = new HashMap<String, JwtProperties.KeyConfig>();
            keys.put("expired-key", expiredConfig);

            var testFilter = createFilterWithKeys(keys, "expired-key");

            String token = JwtTestHelper.createTokenWithKid(SECRET_KEY, "expired-key", "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> testFilter.filter(exchange, chain)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("존재하지 않는 키 ID의 토큰은 IllegalArgumentException을 발생시킨다")
        void should_throwException_when_keyNotFound() {
            String token = JwtTestHelper.createTokenWithKid(SECRET_KEY, "unknown-key", "user1", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> filter.filter(exchange, chain)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Header Injection 방어")
    class HeaderInjection {

        @BeforeEach
        void setUpRoleHierarchy() {
            when(roleHierarchyResolver.resolveEffectiveRoles(any())).thenAnswer(invocation -> {
                List<String> roles = invocation.getArgument(0);
                return Mono.just(roles);
            });
        }

        @Test
        @DisplayName("외부 X-User-* 헤더는 제거되고 JWT의 값으로 교체된다")
        void should_removeExternalUserHeaders_when_nonPublicPath() {
            String token = JwtTestHelper.createValidToken(SECRET_KEY, "real-user", List.of("ROLE_USER"));
            var request = MockServerHttpRequest.get("/api/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-User-Id", "hacker")
                    .header("X-User-Roles", "ROLE_ADMIN")
                    .build();
            var exchange = MockServerWebExchange.from(request);
            when(tokenBlacklistChecker.isBlacklisted(anyString())).thenReturn(Mono.just(false));

            var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
            when(chain.filter(captor.capture())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            var mutatedRequest = captor.getValue().getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Id")).isEqualTo("real-user");
            assertThat(mutatedRequest.getHeaders().getFirst("X-User-Roles")).isEqualTo("ROLE_USER");
        }
    }
}
