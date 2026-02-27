package com.portal.universe.commonlibrary.security.context;

import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import com.portal.universe.commonlibrary.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Set;

/**
 * SecurityContext 기반 권한 검사 유틸리티입니다.
 *
 * <p>서비스 레이어에서 현재 사용자의 Authority를 확인할 때 사용합니다.</p>
 *
 * <p>Role 계층은 JWT의 effectiveRoles (DAG 기반)로 이미 풀려있으므로,
 * ROLE_SUPER_ADMIN을 별도로 나열할 필요 없이 해당 서비스 역할만 지정하면 됩니다.</p>
 *
 * <pre>
 *   // 소유자 또는 블로그 관리자만 허용 (SUPER_ADMIN은 effectiveRoles에 BLOG_ADMIN 포함)
 *   SecurityUtils.assertOwnerOrHasAuthority(
 *       post.getAuthorId(), userId, "ROLE_BLOG_ADMIN", BlogErrorCode.POST_UPDATE_FORBIDDEN);
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * 현재 인증된 사용자가 지정된 Authority 중 하나 이상을 보유하는지 확인합니다.
     *
     * @param authorities 확인할 Authority 목록 (예: "ROLE_BLOG_ADMIN")
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
     * 리소스 소유자인지 검증합니다. 소유자가 아니면 예외를 던집니다.
     *
     * @param resourceOwnerId 리소스 소유자 ID
     * @param currentUserId   현재 사용자 ID
     * @param errorCode       소유자가 아닐 때 던질 ErrorCode
     */
    public static void assertOwner(String resourceOwnerId, String currentUserId, ErrorCode errorCode) {
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new CustomBusinessException(errorCode);
        }
    }

    /**
     * 리소스 소유자이거나 지정된 Authority를 가졌는지 검증합니다.
     * Role 계층은 effectiveRoles로 이미 풀려있으므로 해당 서비스 역할만 지정하면 됩니다.
     *
     * <p>예: assertOwnerOrHasAuthority(ownerId, userId, "ROLE_BLOG_ADMIN", errorCode)
     * → SUPER_ADMIN은 effectiveRoles에 BLOG_ADMIN이 포함되어 자동 통과</p>
     *
     * @param resourceOwnerId   리소스 소유자 ID
     * @param currentUserId     현재 사용자 ID
     * @param requiredAuthority 소유자가 아닐 때 필요한 Authority
     * @param errorCode         소유자도 아니고 권한도 없을 때 던질 ErrorCode
     */
    public static void assertOwnerOrHasAuthority(
            String resourceOwnerId, String currentUserId,
            String requiredAuthority, ErrorCode errorCode) {
        if (resourceOwnerId.equals(currentUserId)) return;
        if (hasAnyAuthority(requiredAuthority)) return;
        throw new CustomBusinessException(errorCode);
    }
}
