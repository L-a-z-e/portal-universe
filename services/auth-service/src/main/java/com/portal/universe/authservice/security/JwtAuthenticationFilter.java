package com.portal.universe.authservice.security;

import com.portal.universe.authservice.service.TokenBlacklistService;
import com.portal.universe.authservice.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Access Token을 검증하고 SecurityContext에 인증 정보를 설정하는 필터입니다.
 * 모든 요청에 대해 한 번씩 실행되며, 블랙리스트에 등록된 토큰은 거부합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정합니다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                // JWT 토큰 검증
                Claims claims = tokenService.validateAccessToken(token);
                String userId = claims.getSubject();
                String roles = claims.get("roles", String.class);

                // Authentication 객체 생성
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roles));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication successful for user: {}", userId);
            } catch (Exception e) {
                log.debug("JWT authentication failed: {}", e.getMessage());
                // 토큰 검증 실패 시 다음 필터로 진행 (인증되지 않은 상태로)
                // Spring Security가 401 응답을 반환함
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * 공개 엔드포인트는 필터를 건너뜁니다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/login/oauth2/") ||
               path.startsWith("/.well-known/") ||
               path.startsWith("/actuator/") ||
               path.equals("/ping") ||
               path.equals("/login") ||
               path.equals("/logout") ||
               path.startsWith("/api/users/signup");
    }
}
