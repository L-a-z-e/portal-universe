package com.portal.universe.commonlibrary.security.context;

/**
 * 인증된 사용자 정보를 담는 record.
 * API Gateway가 JWT 검증 후 전달한 헤더에서 추출합니다.
 *
 * @param uuid     사용자 UUID (X-User-Id)
 * @param name     사용자명 (X-User-Name, URL 디코딩 완료)
 * @param nickname 닉네임 (X-User-Nickname, URL 디코딩 완료)
 */
public record AuthUser(
        String uuid,
        String name,
        String nickname
) {}
