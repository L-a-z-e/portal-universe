package com.portal.universe.notificationservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationType 열거형")
class NotificationTypeTest {

    @Test
    @DisplayName("21개의 enum 값이 존재한다")
    void should_have21Values() {
        assertThat(NotificationType.values()).hasSize(21);
    }

    @Test
    @DisplayName("모든 enum 값은 null이 아닌 defaultMessage를 갖는다")
    void should_haveNonNullDefaultMessage_forAllValues() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(type.getDefaultMessage())
                    .as("NotificationType.%s의 defaultMessage가 null이면 안 됩니다", type.name())
                    .isNotNull()
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("ORDER 관련 타입이 존재한다")
    void should_containOrderTypes() {
        Set<String> names = Arrays.stream(NotificationType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(names).contains(
                "ORDER_CREATED",
                "ORDER_CONFIRMED",
                "ORDER_CANCELLED"
        );
    }

    @Test
    @DisplayName("BLOG 관련 타입이 존재한다")
    void should_containBlogTypes() {
        Set<String> names = Arrays.stream(NotificationType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(names).contains(
                "BLOG_LIKE",
                "BLOG_COMMENT",
                "BLOG_REPLY",
                "BLOG_FOLLOW",
                "BLOG_NEW_POST"
        );
    }

    @Test
    @DisplayName("PRISM 관련 타입이 존재한다")
    void should_containPrismTypes() {
        Set<String> names = Arrays.stream(NotificationType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(names).contains(
                "PRISM_TASK_COMPLETED",
                "PRISM_TASK_FAILED"
        );
    }
}
