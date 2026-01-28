package com.portal.universe.commonlibrary.security.context;

/**
 * API Gateway에서 전달한 사용자 정보를 담는 record.
 *
 * @param uuid     사용자 UUID (X-User-Id)
 * @param name     사용자명 (X-User-Name, URL 디코딩 완료)
 * @param nickname 닉네임 (X-User-Nickname, URL 디코딩 완료)
 */
public record GatewayUser(
        String uuid,
        String name,
        String nickname
) {}
