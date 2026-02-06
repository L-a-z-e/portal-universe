package com.portal.universe.notificationservice.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationConstants")
class NotificationConstantsTest {

    @Test
    @DisplayName("15개 이상의 Kafka topic 상수가 정의되어 있다")
    void should_haveAtLeast15KafkaTopics() {
        List<Field> topicFields = Arrays.stream(NotificationConstants.class.getDeclaredFields())
                .filter(f -> f.getName().startsWith("TOPIC_"))
                .filter(f -> Modifier.isPublic(f.getModifiers()))
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> Modifier.isFinal(f.getModifiers()))
                .collect(Collectors.toList());

        assertThat(topicFields).hasSizeGreaterThanOrEqualTo(15);

        // 주요 토픽들이 존재하는지 확인
        assertThat(NotificationConstants.TOPIC_USER_SIGNUP).isEqualTo("user-signup");
        assertThat(NotificationConstants.TOPIC_ORDER_CREATED).isEqualTo("shopping.order.created");
        assertThat(NotificationConstants.TOPIC_ORDER_CONFIRMED).isEqualTo("shopping.order.confirmed");
        assertThat(NotificationConstants.TOPIC_ORDER_CANCELLED).isEqualTo("shopping.order.cancelled");
        assertThat(NotificationConstants.TOPIC_DELIVERY_SHIPPED).isEqualTo("shopping.delivery.shipped");
        assertThat(NotificationConstants.TOPIC_PAYMENT_COMPLETED).isEqualTo("shopping.payment.completed");
        assertThat(NotificationConstants.TOPIC_PAYMENT_FAILED).isEqualTo("shopping.payment.failed");
        assertThat(NotificationConstants.TOPIC_COUPON_ISSUED).isEqualTo("shopping.coupon.issued");
        assertThat(NotificationConstants.TOPIC_TIMEDEAL_STARTED).isEqualTo("shopping.timedeal.started");
        assertThat(NotificationConstants.TOPIC_BLOG_POST_LIKED).isEqualTo("blog.post.liked");
        assertThat(NotificationConstants.TOPIC_BLOG_POST_COMMENTED).isEqualTo("blog.post.commented");
        assertThat(NotificationConstants.TOPIC_BLOG_COMMENT_REPLIED).isEqualTo("blog.comment.replied");
        assertThat(NotificationConstants.TOPIC_BLOG_USER_FOLLOWED).isEqualTo("blog.user.followed");
        assertThat(NotificationConstants.TOPIC_PRISM_TASK_COMPLETED).isEqualTo("prism.task.completed");
        assertThat(NotificationConstants.TOPIC_PRISM_TASK_FAILED).isEqualTo("prism.task.failed");
    }

    @Test
    @DisplayName("WebSocket queue 경로가 올바르게 정의되어 있다")
    void should_haveCorrectWsQueuePath() {
        assertThat(NotificationConstants.WS_QUEUE_NOTIFICATIONS).isEqualTo("/queue/notifications");
    }

    @Test
    @DisplayName("Redis channel 접두사가 올바르게 정의되어 있다")
    void should_haveCorrectRedisChannelPrefix() {
        assertThat(NotificationConstants.REDIS_CHANNEL_PREFIX).isEqualTo("notification:");
    }

    @Test
    @DisplayName("private 생성자가 정의되어 있어 인스턴스화를 방지한다")
    void should_havePrivateConstructor() throws Exception {
        Constructor<NotificationConstants> constructor = NotificationConstants.class.getDeclaredConstructor();

        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();

        // 리플렉션으로 강제 호출해도 인스턴스는 생성 가능하지만 설계 의도가 private임을 확인
        constructor.setAccessible(true);
        NotificationConstants instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
