package com.portal.universe.commonlibrary.security.context;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * Gateway에서 전달된 멤버십 정보를 하위 서비스에서 쉽게 사용하기 위한 유틸리티입니다.
 *
 * <p>Gateway가 전달하는 X-User-Memberships 헤더의 enriched JSON 형식:</p>
 * <pre>
 *   {"user:blog": {"tier": "PRO", "order": 2}, "seller:shopping": {"tier": "GOLD", "order": 3}}
 * </pre>
 *
 * <p>사용 예시:</p>
 * <pre>
 *   String tier = MembershipContext.getTier(request, "user:blog");
 *   boolean isGold = MembershipContext.hasTierOrAbove(request, "seller:shopping", 3);
 * </pre>
 */
@Slf4j
public final class MembershipContext {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEMBERSHIPS_ATTRIBUTE = "userMemberships";

    private MembershipContext() {}

    /**
     * 현재 요청에서 전체 멤버십 맵을 추출합니다 (enriched 형식).
     *
     * @param request HTTP 요청
     * @return membershipGroup → {tier, order} 맵
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> getMemberships(HttpServletRequest request) {
        Object membershipsAttr = request.getAttribute(MEMBERSHIPS_ATTRIBUTE);
        if (membershipsAttr instanceof String json) {
            try {
                return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Failed to parse memberships JSON: {}", e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 특정 멤버십 그룹의 티어를 조회합니다.
     *
     * @param request HTTP 요청
     * @param membershipGroup 멤버십 그룹 (예: "user:blog", "seller:shopping")
     * @return 티어 키 (예: "PRO"), 없으면 null
     */
    public static String getTier(HttpServletRequest request, String membershipGroup) {
        Map<String, Object> membership = getMemberships(request).get(membershipGroup);
        if (membership != null) {
            Object tier = membership.get("tier");
            return tier != null ? tier.toString() : null;
        }
        return null;
    }

    /**
     * 특정 멤버십 그룹의 sort_order를 조회합니다.
     *
     * @param request HTTP 요청
     * @param membershipGroup 멤버십 그룹
     * @return sort_order 값, 없으면 -1
     */
    public static int getTierOrder(HttpServletRequest request, String membershipGroup) {
        Map<String, Object> membership = getMemberships(request).get(membershipGroup);
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
     * @param request HTTP 요청
     * @param membershipGroup 멤버십 그룹
     * @param requiredOrder 최소 필요 sort_order
     * @return 조건 충족 여부
     */
    public static boolean hasTierOrAbove(HttpServletRequest request, String membershipGroup, int requiredOrder) {
        int currentOrder = getTierOrder(request, membershipGroup);
        return currentOrder >= requiredOrder;
    }
}
