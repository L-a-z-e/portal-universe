package com.portal.universe.authservice.auth.domain;

import java.util.regex.Pattern;

/**
 * 멤버십 그룹 상수 및 유효성 검사 유틸리티.
 * 포맷: {role_scope}:{service} (예: "user:blog", "seller:shopping")
 */
public final class MembershipGroupConstants {

    private MembershipGroupConstants() {}

    // 멤버십 그룹 상수
    public static final String USER_BLOG = "user:blog";
    public static final String USER_SHOPPING = "user:shopping";
    public static final String SELLER_SHOPPING = "seller:shopping";

    // 유효성 검사 패턴: 소문자 영문+숫자, 콜론 구분자
    private static final Pattern GROUP_PATTERN = Pattern.compile("^[a-z][a-z0-9]*:[a-z][a-z0-9]*$");

    /**
     * 멤버십 그룹 포맷 유효성 검사.
     * @param group 검사할 멤버십 그룹 문자열
     * @return 유효하면 true
     */
    public static boolean isValid(String group) {
        return group != null && GROUP_PATTERN.matcher(group).matches();
    }

    /**
     * 유효하지 않은 멤버십 그룹이면 IllegalArgumentException을 던진다.
     */
    public static void validate(String group) {
        if (!isValid(group)) {
            throw new IllegalArgumentException(
                    "Invalid membership group format: '" + group
                            + "'. Expected pattern: {role_scope}:{service} (e.g., 'user:blog')");
        }
    }
}
