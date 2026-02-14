package com.portal.universe.commonlibrary.security.filter;

import com.portal.universe.commonlibrary.security.constants.AuthConstants;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUserArgumentResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Gateway에서 전달한 사용자 정보 헤더를 읽어 SecurityContext를 설정하는 필터입니다.
 *
 * Gateway가 JWT를 검증한 후 다음 헤더를 추가합니다:
 * - X-User-Id: 사용자 UUID
 * - X-User-Roles: 사용자 원본 역할 (쉼표 구분)
 * - X-User-Effective-Roles: Role Hierarchy 상속 포함 전체 유효 역할 (쉼표 구분)
 * - X-User-Memberships: 멤버십 그룹별 enriched JSON
 * - X-User-Nickname: URL 인코딩된 닉네임
 *
 * SecurityContext에는 X-User-Effective-Roles를 우선 사용합니다.
 * AuthUser가 Gateway 헤더의 단일 진실 소스입니다.
 */
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(AuthConstants.Headers.USER_ID);
        String effectiveRoles = request.getHeader(AuthConstants.Headers.USER_EFFECTIVE_ROLES);
        String roles = request.getHeader(AuthConstants.Headers.USER_ROLES);
        String memberships = request.getHeader(AuthConstants.Headers.USER_MEMBERSHIPS);
        String nickname = request.getHeader(AuthConstants.Headers.USER_NICKNAME);
        String username = request.getHeader(AuthConstants.Headers.USER_NAME);

        if (StringUtils.hasText(userId)) {
            // X-User-Effective-Roles 우선, 없으면 X-User-Roles fallback
            String rolesForAuth = StringUtils.hasText(effectiveRoles) ? effectiveRoles : roles;

            log.debug("Gateway authentication - userId: {}, effectiveRoles: {}, memberships: {}",
                    userId, rolesForAuth, memberships);

            // URL 디코딩
            String decodedNickname = decodeHeader(nickname);
            String decodedUsername = decodeHeader(username);

            // AuthUser를 request attribute로 저장 (@CurrentUser resolver에서 사용)
            AuthUser authUser = new AuthUser(userId, decodedUsername, decodedNickname, memberships);
            request.setAttribute(CurrentUserArgumentResolver.AUTH_USER_ATTRIBUTE, authUser);

            // 쉼표 구분된 roles를 복수 Authority로 변환 (effective roles 사용)
            List<SimpleGrantedAuthority> authorities = parseAuthorities(rolesForAuth);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쉼표 구분된 roles 문자열을 복수 SimpleGrantedAuthority로 파싱합니다.
     *
     * @param roles 쉼표 구분 roles (예: "ROLE_USER,ROLE_SHOPPING_SELLER")
     * @return Authority 목록
     */
    private List<SimpleGrantedAuthority> parseAuthorities(String roles) {
        if (!StringUtils.hasText(roles)) {
            return Collections.emptyList();
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private String decodeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
