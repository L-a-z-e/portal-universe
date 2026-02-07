package com.portal.universe.authservice.auth.security;

import com.portal.universe.authservice.auth.service.TokenBlacklistService;
import com.portal.universe.authservice.auth.service.TokenService;
import com.portal.universe.authservice.common.config.PublicPathProperties;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PublicPathProperties publicPathProperties;

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

                // JWT v2: roles 파싱
                List<SimpleGrantedAuthority> authorities = parseAuthorities(claims);
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
     * JWT claims에서 roles를 복수 Authority로 파싱합니다.
     * v2 (List): ["ROLE_USER", "ROLE_SHOPPING_SELLER"] → 복수 Authority
     */
    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> parseAuthorities(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof List<?> rolesArr) {
            return ((List<Object>) rolesArr).stream()
                    .map(Object::toString)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        if (rolesClaim != null) {
            log.warn("Unexpected roles claim type: {}", rolesClaim.getClass().getName());
        }
        return Collections.emptyList();
    }

    /**
     * 공개 엔드포인트는 필터를 건너뜁니다.
     * 경로 목록은 PublicPathProperties로 외부화되어 있습니다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // prefix 매칭
        for (String prefix : publicPathProperties.getSkipJwtParsing()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        // exact 매칭
        for (String exactPath : publicPathProperties.getSkipJwtParsingExact()) {
            if (path.equals(exactPath)) {
                return true;
            }
        }

        return false;
    }
}
