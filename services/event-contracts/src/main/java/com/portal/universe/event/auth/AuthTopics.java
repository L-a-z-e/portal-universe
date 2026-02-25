package com.portal.universe.event.auth;

/**
 * Auth 도메인 Kafka topic 상수.
 *
 * <p>SSOT: {@code services/event-contracts/schemas/com/portal/universe/event/auth/*.avsc}</p>
 */
public final class AuthTopics {

    public static final String USER_SIGNED_UP = "auth.user.signed-up";
    public static final String ROLE_ASSIGNED = "auth.role.assigned";

    private AuthTopics() {}
}
