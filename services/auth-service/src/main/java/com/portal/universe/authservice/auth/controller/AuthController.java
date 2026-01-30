package com.portal.universe.authservice.auth.controller;

import com.portal.universe.authservice.auth.dto.LoginRequest;
import com.portal.universe.authservice.auth.dto.LoginResponse;
import com.portal.universe.authservice.auth.dto.LogoutRequest;
import com.portal.universe.authservice.auth.dto.RefreshRequest;
import com.portal.universe.authservice.auth.dto.RefreshResponse;
import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.authservice.user.repository.UserRepository;
import com.portal.universe.authservice.common.util.TokenUtils;
import com.portal.universe.authservice.auth.service.LoginAttemptService;
import com.portal.universe.authservice.auth.service.RefreshTokenService;
import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.response.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * 인증 관련 API 컨트롤러
 * 로그인, 토큰 갱신, 로그아웃 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * 일반 로그인 API
     * email/password로 인증 후 Access Token과 Refresh Token을 발급합니다.
     *
     * @param request 로그인 요청 (email, password)
     * @param servletRequest HTTP 요청 (IP 추출용)
     * @return 로그인 응답 (accessToken, refreshToken, expiresIn)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String clientIp = getClientIp(servletRequest);
        String loginKey = clientIp + ":" + request.email();

        log.info("Login attempt for email: {} from IP: {}", request.email(), clientIp);

        // 1. 잠금 상태 확인
        if (loginAttemptService.isBlocked(loginKey)) {
            long remainingSeconds = loginAttemptService.getRemainingLockTime(loginKey);
            int remainingMinutes = (int) Math.ceil(remainingSeconds / 60.0);

            log.warn("Login blocked for key: {} (remaining: {} seconds)", loginKey, remainingSeconds);

            throw new CustomBusinessException(
                    AuthErrorCode.ACCOUNT_TEMPORARILY_LOCKED,
                    String.valueOf(remainingMinutes)
            );
        }

        // 2. 사용자 조회 (프로필 포함 - JWT에 nickname 포함 위해)
        User user = userRepository.findByEmailWithProfile(request.email())
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(loginKey);
                    return new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(loginKey);
            throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 4. 로그인 성공 - 실패 기록 초기화
        loginAttemptService.recordSuccess(loginKey);

        // 5. Access Token 발급
        String accessToken = tokenService.generateAccessToken(user);

        // 6. Refresh Token 발급 및 Redis 저장
        String refreshToken = tokenService.generateRefreshToken(user);
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        // 7. Refresh Token을 HttpOnly Cookie로 설정
        setRefreshTokenCookie(servletResponse, refreshToken);

        log.info("Login successful for user: {} from IP: {}", user.getUuid(), clientIp);

        // 응답 body에도 refreshToken 포함 (하위 호환)
        LoginResponse response = new LoginResponse(accessToken, refreshToken, 900);  // 15분 = 900초
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 토큰 갱신 API
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     *
     * @param request 토큰 갱신 요청 (refreshToken)
     * @return 토큰 갱신 응답 (accessToken, expiresIn)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String cookieRefreshToken,
            HttpServletResponse servletResponse) {
        log.info("Token refresh attempt");

        // Cookie 우선, Body fallback
        String refreshToken = resolveRefreshToken(cookieRefreshToken, request);
        if (refreshToken == null) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            // 1. Refresh Token 검증 (JWT 서명 검증)
            Claims claims = tokenService.validateRefreshToken(refreshToken);
            String userId = claims.getSubject();

            // 2. 사용자 정보 조회 (프로필 포함 - JWT에 nickname 포함 위해)
            User user = userRepository.findByUuidWithProfile(userId)
                    .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

            // 3. 새로운 Access Token 발급
            String accessToken = tokenService.generateAccessToken(user);

            // 4. Refresh Token Rotation: 새 refresh token 발급 + 원자적 교체
            String newRefreshToken = tokenService.generateRefreshToken(user);
            if (!refreshTokenService.rotateRefreshToken(userId, refreshToken, newRefreshToken)) {
                // 원자적 교체 실패 (이미 다른 요청에서 교체됨)
                throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }

            // 5. 새 Refresh Token을 HttpOnly Cookie로 설정
            setRefreshTokenCookie(servletResponse, newRefreshToken);

            log.info("Token refresh successful for user: {}", user.getUuid());

            RefreshResponse response = new RefreshResponse(accessToken, newRefreshToken, 900);  // 15분 = 900초
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * 로그아웃 API
     * Access Token을 블랙리스트에 추가하고, Refresh Token을 Redis에서 삭제합니다.
     *
     * @param authorization Authorization 헤더 (Bearer {accessToken})
     * @param request 로그아웃 요청 (refreshToken)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String cookieRefreshToken,
            HttpServletResponse servletResponse) {

        log.info("Logout attempt");

        // 1. Authorization 헤더에서 Access Token 추출
        String accessToken = TokenUtils.extractBearerToken(authorization);

        try {
            // 2. Access Token에서 userId 추출 (만료된 토큰도 허용, 서명은 검증)
            Claims claims = tokenService.parseClaimsAllowExpired(accessToken);
            String userId = claims.getSubject();

            // 3. Access Token 블랙리스트 추가 (아직 유효한 경우에만)
            long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
            if (remainingExpiration > 0) {
                tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);
            }

            // 4. Refresh Token Redis에서 삭제 (항상 실행)
            refreshTokenService.deleteRefreshToken(userId);

            // 5. Refresh Token Cookie 삭제
            clearRefreshTokenCookie(servletResponse);

            log.info("Logout successful for user: {}", userId);

            return ResponseEntity.ok(
                    ApiResponse.success(Map.of("message", "로그아웃 성공")));
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            throw new CustomBusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private static final String REFRESH_TOKEN_COOKIE_NAME = "portal_refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/";

    /**
     * Cookie 우선, Body fallback으로 Refresh Token을 결정합니다.
     */
    private String resolveRefreshToken(String cookieToken, RefreshRequest request) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        return null;
    }

    /**
     * Refresh Token을 HttpOnly Cookie로 설정합니다.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Refresh Token Cookie를 삭제합니다.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 프록시나 로드 밸런서를 거친 경우 X-Forwarded-For 헤더를 우선 확인합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

}
