package com.portal.universe.commonlibrary.security.constants;

/**
 * 인증/인가 관련 상수를 중앙 관리합니다.
 * Gateway, Auth-service 등 여러 서비스에서 공통으로 사용됩니다.
 */
public final class AuthConstants {

    private AuthConstants() {
        // 인스턴스 생성 방지
    }

    /** HTTP 헤더 상수 */
    public static final class Headers {
        public static final String BEARER_PREFIX = "Bearer ";
        public static final String USER_ID = "X-User-Id";
        public static final String USER_ROLES = "X-User-Roles";
        public static final String USER_MEMBERSHIPS = "X-User-Memberships";
        public static final String USER_NICKNAME = "X-User-Nickname";
        public static final String USER_NAME = "X-User-Name";
        public static final String AUTH_ERROR = "X-Auth-Error";

        private Headers() {}
    }

    /** Redis 키 prefix 상수 */
    public static final class RedisKeys {
        public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
        public static final String BLACKLIST_PREFIX = "blacklist:";
        public static final String LOGIN_ATTEMPT_COUNT_PREFIX = "login_attempt:count:";
        public static final String LOGIN_ATTEMPT_LOCK_PREFIX = "login_attempt:lock:";

        private RedisKeys() {}
    }

    /** JWT Claims 키 상수 */
    public static final class Claims {
        public static final String ROLES = "roles";
        public static final String MEMBERSHIPS = "memberships";
        public static final String EMAIL = "email";
        public static final String NICKNAME = "nickname";
        public static final String USERNAME = "username";

        private Claims() {}
    }

    /** 기본 역할 상수 */
    public static final class Roles {
        public static final String ROLE_USER = "ROLE_USER";
        public static final String ROLE_ADMIN = "ROLE_ADMIN";
        public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
        public static final String ROLE_SELLER = "ROLE_SELLER";

        private Roles() {}
    }

    /** Cookie 상수 */
    public static final class Cookies {
        public static final String REFRESH_TOKEN_NAME = "portal_refresh_token";
        public static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

        private Cookies() {}
    }
}
