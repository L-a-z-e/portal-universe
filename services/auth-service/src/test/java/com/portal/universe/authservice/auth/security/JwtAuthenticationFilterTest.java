package com.portal.universe.authservice.auth.security;

import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.authservice.common.config.PublicPathProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Test")
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private PublicPathProperties publicPathProperties;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("유효한 토큰이면 SecurityContext에 올바른 userId와 roles로 Authentication을 설정한다")
        void should_setAuthenticationWithCorrectUserIdAndRoles_when_tokenIsValid() throws ServletException, IOException {
            // given
            String token = "valid.jwt.token";
            String userId = "user-uuid-123";
            request.addHeader("Authorization", "Bearer " + token);

            Claims claims = createClaims(userId, List.of("ROLE_USER", "ROLE_SELLER"));

            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
            when(tokenService.validateAccessToken(token)).thenReturn(claims);

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(userId);
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_SELLER");

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 SecurityContext를 설정하지 않고 chain을 진행한다")
        void should_continueChainWithoutSecurityContext_when_noToken() throws ServletException, IOException {
            // given - no Authorization header

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenService, tokenBlacklistService);
        }

        @Test
        @DisplayName("블랙리스트에 등록된 토큰이면 SecurityContext를 설정하지 않고 chain을 진행한다")
        void should_continueChainWithoutSecurityContext_when_tokenIsBlacklisted() throws ServletException, IOException {
            // given
            String token = "blacklisted.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);

            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenService);
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 SecurityContext를 설정하지 않고 chain을 진행한다")
        void should_continueChainWithoutSecurityContext_when_tokenIsInvalid() throws ServletException, IOException {
            // given
            String token = "invalid.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);

            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
            when(tokenService.validateAccessToken(token)).thenThrow(new RuntimeException("Invalid token"));

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("만료된 토큰이면 SecurityContext를 설정하지 않고 chain을 진행한다")
        void should_continueChainWithoutSecurityContext_when_tokenIsExpired() throws ServletException, IOException {
            // given
            String token = "expired.jwt.token";
            request.addHeader("Authorization", "Bearer " + token);

            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
            when(tokenService.validateAccessToken(token))
                    .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Token expired"));

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Bearer 접두사가 없는 Authorization 헤더이면 토큰을 추출하지 않는다")
        void should_notExtractToken_when_authorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
            // given
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(tokenService, tokenBlacklistService);
        }

        @Test
        @DisplayName("roles claim이 없으면 빈 authorities로 Authentication을 설정한다")
        void should_setEmptyAuthorities_when_rolesClaimIsMissing() throws ServletException, IOException {
            // given
            String token = "valid.jwt.token";
            String userId = "user-uuid-123";
            request.addHeader("Authorization", "Bearer " + token);

            Claims claims = createClaims(userId, null);

            when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
            when(tokenService.validateAccessToken(token)).thenReturn(claims);

            // when
            filter.doFilterInternal(request, response, filterChain);

            // then
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(userId);
            assertThat(authentication.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("공개 경로 prefix와 일치하면 true를 반환한다")
        void should_returnTrue_when_pathMatchesPublicPrefix() {
            // given
            request.setRequestURI("/api/auth/login");
            when(publicPathProperties.getSkipJwtParsing()).thenReturn(List.of("/api/auth/"));

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("exact match 경로와 일치하면 true를 반환한다")
        void should_returnTrue_when_pathMatchesExactPath() {
            // given
            request.setRequestURI("/ping");
            when(publicPathProperties.getSkipJwtParsing()).thenReturn(List.of());
            when(publicPathProperties.getSkipJwtParsingExact()).thenReturn(List.of("/ping", "/login"));

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("보호된 경로이면 false를 반환한다")
        void should_returnFalse_when_pathIsProtected() {
            // given
            request.setRequestURI("/api/v1/users/me");
            when(publicPathProperties.getSkipJwtParsing()).thenReturn(List.of("/api/auth/"));
            when(publicPathProperties.getSkipJwtParsingExact()).thenReturn(List.of("/ping"));

            // when
            boolean result = filter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }
    }

    // ========== Helper Methods ==========

    private Claims createClaims(String subject, List<String> roles) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        if (roles != null) {
            when(claims.get("roles")).thenReturn(roles);
        }
        return claims;
    }
}
