package com.portal.universe.commonlibrary.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SecurityContext 기반 권한 검사 유틸리티입니다.
 *
 * <p>서비스 레이어에서 현재 사용자의 Authority를 확인할 때 사용합니다.</p>
 *
 * <pre>
 *   // 관리자 bypass 패턴
 *   if (!post.getAuthorId().equals(userId)
 *       && !SecurityUtils.hasAnyAuthority("ROLE_BLOG_ADMIN", "ROLE_SUPER_ADMIN")) {
 *       throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
 *   }
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 현재 인증된 사용자가 지정된 Authority 중 하나 이상을 보유하는지 확인합니다.
     *
     * @param authorities 확인할 Authority 목록 (예: "ROLE_BLOG_ADMIN", "ROLE_SUPER_ADMIN")
     * @return 하나 이상 보유 시 true
     */
    public static boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Set<String> requiredSet = Set.of(authorities);
        Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();

        return userAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredSet::contains);
    }

    /**
     * 현재 인증된 사용자가 시스템 관리자(SUPER_ADMIN)인지 확인합니다.
     */
    public static boolean isSuperAdmin() {
        return hasAnyAuthority("ROLE_SUPER_ADMIN");
    }

    /**
     * 현재 인증된 사용자가 특정 서비스의 관리자인지 확인합니다.
     *
     * @param service 서비스명 (예: "BLOG", "SHOPPING")
     * @return 해당 서비스 관리자 또는 SUPER_ADMIN이면 true
     */
    public static boolean isServiceAdmin(String service) {
        return hasAnyAuthority("ROLE_" + service.toUpperCase() + "_ADMIN", "ROLE_SUPER_ADMIN");
    }
}
