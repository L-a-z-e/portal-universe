package com.portal.universe.commonlibrary.security.filter;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Gateway에서 전달한 사용자 정보 헤더를 읽어 SecurityContext를 설정하는 필터입니다.
 *
 * Gateway가 JWT를 검증한 후 다음 헤더를 추가합니다:
 * - X-User-Id: 사용자 UUID
 * - X-User-Roles: 사용자 권한 (쉼표 구분, 예: ROLE_USER,ROLE_SELLER)
 * - X-User-Memberships: 서비스별 멤버십 JSON (예: {"shopping":"PREMIUM"})
 * - X-User-Nickname: URL 인코딩된 닉네임
 *
 * 이 필터는 헤더를 읽어서 Spring Security의 SecurityContext에 인증 정보를 설정합니다.
 */
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String USER_MEMBERSHIPS_HEADER = "X-User-Memberships";
    public static final String USER_NICKNAME_HEADER = "X-User-Nickname";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String roles = request.getHeader(USER_ROLES_HEADER);
        String memberships = request.getHeader(USER_MEMBERSHIPS_HEADER);
        String nickname = request.getHeader(USER_NICKNAME_HEADER);

        if (StringUtils.hasText(userId)) {
            log.debug("Gateway authentication - userId: {}, roles: {}, memberships: {}", userId, roles, memberships);

            // nickname을 request attribute로 저장 (하위 서비스에서 활용)
            if (StringUtils.hasText(nickname)) {
                request.setAttribute("userNickname", nickname);
            }

            // memberships JSON을 request attribute로 저장 (하위 서비스에서 활용)
            if (StringUtils.hasText(memberships)) {
                request.setAttribute("userMemberships", memberships);
            }

            // 쉼표 구분된 roles를 복수 Authority로 변환
            List<SimpleGrantedAuthority> authorities = parseAuthorities(roles);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쉼표 구분된 roles 문자열을 복수 SimpleGrantedAuthority로 파싱합니다.
     * v1 호환: 단일 role 문자열도 지원합니다.
     *
     * @param roles 쉼표 구분 roles (예: "ROLE_USER,ROLE_SELLER")
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
}
