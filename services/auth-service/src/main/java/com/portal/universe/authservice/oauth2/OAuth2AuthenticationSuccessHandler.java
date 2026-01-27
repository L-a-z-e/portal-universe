package com.portal.universe.authservice.oauth2;

import com.portal.universe.authservice.user.domain.User;
import com.portal.universe.authservice.service.RefreshTokenService;
import com.portal.universe.authservice.service.TokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 소셜 로그인 성공 시 처리하는 핸들러입니다.
 * 로그인 성공 후 프론트엔드로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.base-url:http://localhost:30000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            log.debug("Response has already been committed.");
            return;
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        log.info("OAuth2 로그인 성공 - email: {}, uuid: {}", user.getEmail(), user.getUuid());

        // JWT 토큰 발급
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        // Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        // URL Fragment로 토큰 전달
        // 프론트엔드에서 #access_token=...&refresh_token=... 형태로 수신
        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/oauth2/callback")
                .fragment("access_token=" + accessToken +
                         "&refresh_token=" + refreshToken +
                         "&expires_in=900")
                .build().toUriString();

        clearAuthenticationAttributes(request);

        // Stateless - 세션 사용 안 함
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
