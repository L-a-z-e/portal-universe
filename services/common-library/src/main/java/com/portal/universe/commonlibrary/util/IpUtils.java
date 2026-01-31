package com.portal.universe.commonlibrary.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 주소 추출 유틸리티
 * 프록시나 로드 밸런서 환경에서 실제 클라이언트 IP를 추출합니다.
 */
public class IpUtils {

    private IpUtils() {
        // Utility class
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * 프록시나 로드 밸런서를 거친 경우 X-Forwarded-For 헤더를 우선 확인합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
