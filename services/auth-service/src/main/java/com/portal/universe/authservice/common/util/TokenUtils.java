package com.portal.universe.authservice.common.util;

import com.portal.universe.authservice.common.exception.AuthErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;

/**
 * JWT 토큰 관련 유틸리티 클래스
 *
 * <p>Authorization 헤더에서 Bearer 토큰을 추출하는 등의
 * 토큰 처리 관련 공통 기능을 제공합니다.</p>
 *
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * String token = TokenUtils.extractBearerToken(authorization);
 * }</pre>
 *
 * <h3>Bearer 토큰이란?</h3>
 * <p>OAuth 2.0에서 정의한 토큰 전달 방식으로,
 * "Bearer"는 "소지자"라는 의미입니다.
 * 이 토큰을 가진 사람(bearer)은 토큰에 명시된 권한을 가집니다.</p>
 *
 * <p>형식: {@code Authorization: Bearer <token>}</p>
 */
public final class TokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    // 유틸리티 클래스는 인스턴스화 방지
    private TokenUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출합니다.
     *
     * @param authorization Authorization 헤더 값 (예: "Bearer eyJhbGciOiJI...")
     * @return JWT 토큰 문자열 (Bearer 접두사 제거됨)
     * @throws CustomBusinessException 헤더가 null이거나 Bearer 형식이 아닌 경우
     */
    public static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    /**
     * Authorization 헤더가 Bearer 토큰 형식인지 확인합니다.
     *
     * @param authorization Authorization 헤더 값
     * @return Bearer 형식이면 true, 아니면 false
     */
    public static boolean isBearerToken(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX);
    }
}
