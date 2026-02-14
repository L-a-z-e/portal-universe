package com.portal.universe.commonlibrary.security.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * AuthUser에 포함된 멤버십 정보를 하위 서비스에서 쉽게 사용하기 위한 유틸리티입니다.
 *
 * <p>Gateway가 전달하는 X-User-Memberships 헤더의 enriched JSON 형식:</p>
 * <pre>
 *   {"user:blog": {"tier": "PRO", "order": 2}, "seller:shopping": {"tier": "GOLD", "order": 3}}
 * </pre>
 *
 * <p>사용 예시:</p>
 * <pre>
 *   String tier = MembershipContext.getTier(authUser, "user:blog");
 *   boolean isGold = MembershipContext.hasTierOrAbove(authUser, "seller:shopping", 3);
 * </pre>
 */
@Slf4j
public final class MembershipContext {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MembershipContext() {}

    /**
     * AuthUser에서 전체 멤버십 맵을 추출합니다 (enriched 형식).
     *
     * @param user 인증된 사용자 정보
     * @return membershipGroup → {tier, order} 맵
     */
    public static Map<String, Map<String, Object>> getMemberships(AuthUser user) {
        if (user == null || user.memberships() == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(user.memberships(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse memberships JSON: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * 특정 멤버십 그룹의 티어를 조회합니다.
     *
     * @param user            인증된 사용자 정보
     * @param membershipGroup 멤버십 그룹 (예: "user:blog", "seller:shopping")
     * @return 티어 키 (예: "PRO"), 없으면 null
     */
    public static String getTier(AuthUser user, String membershipGroup) {
        Map<String, Object> membership = getMemberships(user).get(membershipGroup);
        if (membership != null) {
            Object tier = membership.get("tier");
            return tier != null ? tier.toString() : null;
        }
        return null;
    }

    /**
     * 특정 멤버십 그룹의 sort_order를 조회합니다.
     *
     * @param user            인증된 사용자 정보
     * @param membershipGroup 멤버십 그룹
     * @return sort_order 값, 없으면 -1
     */
    public static int getTierOrder(AuthUser user, String membershipGroup) {
        Map<String, Object> membership = getMemberships(user).get(membershipGroup);
        if (membership != null) {
            Object order = membership.get("order");
            if (order instanceof Number num) {
                return num.intValue();
            }
        }
        return -1;
    }

    /**
     * 특정 멤버십 그룹에서 지정 순서 이상인지 확인합니다.
     * sort_order 값으로 비교합니다 (값이 클수록 상위 티어).
     *
     * @param user            인증된 사용자 정보
     * @param membershipGroup 멤버십 그룹
     * @param requiredOrder   최소 필요 sort_order
     * @return 조건 충족 여부
     */
    public static boolean hasTierOrAbove(AuthUser user, String membershipGroup, int requiredOrder) {
        int currentOrder = getTierOrder(user, membershipGroup);
        return currentOrder >= requiredOrder;
    }
}
