package com.portal.universe.authservice.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.authservice.auth.dto.LoginRequest;
import com.portal.universe.authservice.auth.dto.LoginResponse;
import com.portal.universe.authservice.auth.dto.LogoutRequest;
import com.portal.universe.authservice.auth.dto.RefreshRequest;
import com.portal.universe.authservice.auth.dto.RefreshResponse;
import com.portal.universe.authservice.auth.service.LoginAttemptService;
import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.authservice.common.config.JwtProperties;
import com.portal.universe.authservice.common.util.RefreshTokenCookieHelper;
import com.portal.universe.authservice.auth.security.JwtAuthenticationFilter;
import com.portal.universe.authservice.password.config.PasswordPolicyProperties;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Unit Test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    PasswordEncoder passwordEncoder;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    LoginAttemptService loginAttemptService;

    @MockitoBean
    JwtProperties jwtProperties;

    @MockitoBean
    PasswordPolicyProperties passwordPolicyProperties;

    @MockitoBean
    RefreshTokenCookieHelper cookieHelper;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL = "/api/v1/auth/logout";
    private static final String PASSWORD_POLICY_URL = "/api/v1/auth/password-policy";

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("should_returnLoginResponse_when_validCredentials")
        void should_returnLoginResponse_when_validCredentials() throws Exception {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "password123");

            User mockUser = mock(User.class);
            when(mockUser.getPassword()).thenReturn("encoded-password");
            when(mockUser.getUuid()).thenReturn("test-uuid");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmailWithProfile("user@test.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(tokenService.generateAccessToken(mockUser)).thenReturn("access-token");
            when(tokenService.generateRefreshToken(mockUser)).thenReturn("refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900));
        }

        @Test
        @DisplayName("should_returnError_when_accountIsLocked")
        void should_returnError_when_accountIsLocked() throws Exception {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "password123");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(true);
            when(loginAttemptService.getRemainingLockTime(anyString())).thenReturn(120L);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_returnError_when_userNotFound")
        void should_returnError_when_userNotFound() throws Exception {
            // given
            LoginRequest request = new LoginRequest("unknown@test.com", "password123");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmailWithProfile("unknown@test.com")).thenReturn(Optional.empty());

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_returnError_when_invalidPassword")
        void should_returnError_when_invalidPassword() throws Exception {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "wrong-password");

            User mockUser = mock(User.class);
            when(mockUser.getPassword()).thenReturn("encoded-password");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmailWithProfile("user@test.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_returnBadRequest_when_emailIsBlank")
        void should_returnBadRequest_when_emailIsBlank() throws Exception {
            // given
            LoginRequest request = new LoginRequest("", "password123");

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_callRecordFailure_when_loginFailed")
        void should_callRecordFailure_when_loginFailed() throws Exception {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "wrong-password");

            User mockUser = mock(User.class);
            when(mockUser.getPassword()).thenReturn("encoded-password");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmailWithProfile("user@test.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(loginAttemptService).recordFailure(anyString());
        }

        @Test
        @DisplayName("should_callRecordSuccess_when_loginSucceeded")
        void should_callRecordSuccess_when_loginSucceeded() throws Exception {
            // given
            LoginRequest request = new LoginRequest("user@test.com", "password123");

            User mockUser = mock(User.class);
            when(mockUser.getPassword()).thenReturn("encoded-password");
            when(mockUser.getUuid()).thenReturn("test-uuid");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmailWithProfile("user@test.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(tokenService.generateAccessToken(mockUser)).thenReturn("access-token");
            when(tokenService.generateRefreshToken(mockUser)).thenReturn("refresh-token");
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(loginAttemptService).recordSuccess(anyString());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("should_returnNewTokens_when_validRefreshTokenInBody")
        void should_returnNewTokens_when_validRefreshTokenInBody() throws Exception {
            // given
            RefreshRequest request = new RefreshRequest("valid-refresh-token");

            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("test-uuid");

            User mockUser = mock(User.class);
            when(mockUser.getUuid()).thenReturn("test-uuid");

            when(tokenService.validateRefreshToken("valid-refresh-token")).thenReturn(mockClaims);
            when(userRepository.findByUuidWithProfile("test-uuid")).thenReturn(Optional.of(mockUser));
            when(tokenService.generateAccessToken(mockUser)).thenReturn("new-access-token");
            when(tokenService.generateRefreshToken(mockUser)).thenReturn("new-refresh-token");
            when(refreshTokenService.rotateRefreshToken("test-uuid", "valid-refresh-token", "new-refresh-token"))
                    .thenReturn(true);
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

            // when & then
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                    .andExpect(jsonPath("$.data.expiresIn").value(900));
        }

        @Test
        @DisplayName("should_returnNewTokens_when_validRefreshTokenInCookie")
        void should_returnNewTokens_when_validRefreshTokenInCookie() throws Exception {
            // given
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("test-uuid");

            User mockUser = mock(User.class);
            when(mockUser.getUuid()).thenReturn("test-uuid");

            when(tokenService.validateRefreshToken("cookie-refresh-token")).thenReturn(mockClaims);
            when(userRepository.findByUuidWithProfile("test-uuid")).thenReturn(Optional.of(mockUser));
            when(tokenService.generateAccessToken(mockUser)).thenReturn("new-access-token");
            when(tokenService.generateRefreshToken(mockUser)).thenReturn("new-refresh-token");
            when(refreshTokenService.rotateRefreshToken("test-uuid", "cookie-refresh-token", "new-refresh-token"))
                    .thenReturn(true);
            when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);

            // when & then
            mockMvc.perform(post(REFRESH_URL)
                            .cookie(new Cookie(RefreshTokenCookieHelper.COOKIE_NAME, "cookie-refresh-token"))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("should_returnError_when_noRefreshTokenProvided")
        void should_returnError_when_noRefreshTokenProvided() throws Exception {
            // when & then
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_returnError_when_rotationFails")
        void should_returnError_when_rotationFails() throws Exception {
            // given
            RefreshRequest request = new RefreshRequest("stale-refresh-token");

            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("test-uuid");

            User mockUser = mock(User.class);
            when(mockUser.getUuid()).thenReturn("test-uuid");

            when(tokenService.validateRefreshToken("stale-refresh-token")).thenReturn(mockClaims);
            when(userRepository.findByUuidWithProfile("test-uuid")).thenReturn(Optional.of(mockUser));
            when(tokenService.generateAccessToken(mockUser)).thenReturn("new-access-token");
            when(tokenService.generateRefreshToken(mockUser)).thenReturn("new-refresh-token");
            when(refreshTokenService.rotateRefreshToken("test-uuid", "stale-refresh-token", "new-refresh-token"))
                    .thenReturn(false);

            // when & then
            mockMvc.perform(post(REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("should_returnSuccess_when_validLogout")
        void should_returnSuccess_when_validLogout() throws Exception {
            // given
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("test-uuid");

            when(tokenService.parseClaimsAllowExpired("valid-access-token")).thenReturn(mockClaims);
            when(tokenService.getRemainingExpiration("valid-access-token")).thenReturn(300000L);

            // when & then
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer valid-access-token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.message").value("로그아웃 성공"));

            verify(tokenBlacklistService).addToBlacklist("valid-access-token", 300000L);
            verify(refreshTokenService).deleteRefreshToken("test-uuid");
        }

        @Test
        @DisplayName("should_skipBlacklist_when_tokenAlreadyExpired")
        void should_skipBlacklist_when_tokenAlreadyExpired() throws Exception {
            // given
            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("test-uuid");

            when(tokenService.parseClaimsAllowExpired("expired-token")).thenReturn(mockClaims);
            when(tokenService.getRemainingExpiration("expired-token")).thenReturn(0L);

            // when & then
            mockMvc.perform(post(LOGOUT_URL)
                            .header("Authorization", "Bearer expired-token")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(refreshTokenService).deleteRefreshToken("test-uuid");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/password-policy")
    class GetPasswordPolicy {

        @Test
        @DisplayName("should_returnPasswordPolicy_when_requested")
        void should_returnPasswordPolicy_when_requested() throws Exception {
            // given
            when(passwordPolicyProperties.getMinLength()).thenReturn(8);
            when(passwordPolicyProperties.getMaxLength()).thenReturn(128);
            when(passwordPolicyProperties.isRequireUppercase()).thenReturn(true);
            when(passwordPolicyProperties.isRequireLowercase()).thenReturn(true);
            when(passwordPolicyProperties.isRequireDigit()).thenReturn(true);
            when(passwordPolicyProperties.isRequireSpecialChar()).thenReturn(true);
            when(passwordPolicyProperties.getSpecialChars()).thenReturn("!@#$%^&*()");
            when(passwordPolicyProperties.isPreventSequential()).thenReturn(false);
            when(passwordPolicyProperties.isPreventUserInfo()).thenReturn(false);
            when(passwordPolicyProperties.getHistoryCount()).thenReturn(0);

            // when & then
            mockMvc.perform(get(PASSWORD_POLICY_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.minLength").value(8))
                    .andExpect(jsonPath("$.data.maxLength").value(128))
                    .andExpect(jsonPath("$.data.requirements").isArray());
        }
    }
}
