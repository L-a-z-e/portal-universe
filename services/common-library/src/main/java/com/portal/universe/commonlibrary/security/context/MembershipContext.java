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
 * <p>사용 예시:</p>
 * <pre>
 *   String tier = MembershipContext.getTier(request, "shopping");
 *   boolean isPremium = MembershipContext.hasTierOrAbove(request, "shopping", "PREMIUM");
 * </pre>
 */
@Slf4j
public final class MembershipContext {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEMBERSHIPS_ATTRIBUTE = "userMemberships";

    private static final Map<String, Integer> TIER_ORDER = Map.of(
            "FREE", 0,
            "BASIC", 1,
            "PREMIUM", 2,
            "VIP", 3
    );

    private MembershipContext() {}

    /**
     * 현재 요청에서 전체 멤버십 맵을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 서비스명 → 티어키 맵 (예: {"shopping": "PREMIUM", "blog": "FREE"})
     */
    public static Map<String, String> getMemberships(HttpServletRequest request) {
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
     * 특정 서비스의 멤버십 티어를 조회합니다.
     *
     * @param request HTTP 요청
     * @param serviceName 서비스 이름 (예: "shopping")
     * @return 티어 키 (예: "PREMIUM"), 없으면 "FREE"
     */
    public static String getTier(HttpServletRequest request, String serviceName) {
        return getMemberships(request).getOrDefault(serviceName, "FREE");
    }

    /**
     * 특정 서비스에서 지정 티어 이상인지 확인합니다.
     *
     * @param request HTTP 요청
     * @param serviceName 서비스 이름
     * @param requiredTier 최소 필요 티어 (예: "PREMIUM")
     * @return 조건 충족 여부
     */
    public static boolean hasTierOrAbove(HttpServletRequest request, String serviceName, String requiredTier) {
        String currentTier = getTier(request, serviceName);
        int currentOrder = TIER_ORDER.getOrDefault(currentTier.toUpperCase(), 0);
        int requiredOrder = TIER_ORDER.getOrDefault(requiredTier.toUpperCase(), 0);
        return currentOrder >= requiredOrder;
    }
}
