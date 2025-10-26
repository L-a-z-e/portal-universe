package com.portal.universe.common.event;

/**
 * 신규 사용자 가입 시 Kafka를 통해 발행되는 이벤트 DTO입니다.
 * 이 이벤트를 구독하는 다른 마이크로서비스(예: 쇼핑 서비스)는
 * 이 정보를 받아 자신의 데이터베이스에 사용자 정보를 동기화할 수 있습니다.
 *
 * @param userId 생성된 사용자의 고유 ID
 * @param email 사용자의 이메일
 * @param name 사용자의 이름
 */
public record UserSignedUpEvent(
        String userId,
        String email,
        String name
) {}
