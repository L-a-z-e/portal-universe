package com.portal.universe.commonlibrary.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 주소 추출 유틸리티.
 *
 * <p>백엔드 서비스에서 사용됩니다. Gateway가 X-Forwarded-For를 설정하므로
 * 이 헤더를 신뢰합니다. Gateway 자체의 IP 해석은 {@code maxTrustedIndex(1)}로 처리됩니다.</p>
 */
public class IpUtils {

    private IpUtils() {
        // Utility class
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * Gateway가 설정한 X-Forwarded-For를 우선 확인하고, 없으면 remoteAddr을 사용합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP 사용
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }

        ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }
}
