package com.portal.universe.notificationservice.common.constants;

import com.portal.universe.event.auth.AuthTopics;
import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.prism.PrismTopics;
import com.portal.universe.event.shopping.ShoppingTopics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationConstants & Domain Topics")
class NotificationConstantsTest {

    @Test
    @DisplayName("도메인 Topics 클래스에 올바른 topic 이름이 정의되어 있다")
    void should_haveDomainTopicsWithCorrectValues() {
        // Auth
        assertThat(AuthTopics.USER_SIGNED_UP).isEqualTo("auth.user.signed-up");

        // Shopping
        assertThat(ShoppingTopics.ORDER_CREATED).isEqualTo("shopping.order.created");
        assertThat(ShoppingTopics.ORDER_CONFIRMED).isEqualTo("shopping.order.confirmed");
        assertThat(ShoppingTopics.ORDER_CANCELLED).isEqualTo("shopping.order.cancelled");
        assertThat(ShoppingTopics.DELIVERY_SHIPPED).isEqualTo("shopping.delivery.shipped");
        assertThat(ShoppingTopics.PAYMENT_COMPLETED).isEqualTo("shopping.payment.completed");
        assertThat(ShoppingTopics.PAYMENT_FAILED).isEqualTo("shopping.payment.failed");
        assertThat(ShoppingTopics.COUPON_ISSUED).isEqualTo("shopping.coupon.issued");
        assertThat(ShoppingTopics.TIMEDEAL_STARTED).isEqualTo("shopping.timedeal.started");
        assertThat(ShoppingTopics.INVENTORY_RESERVED).isEqualTo("shopping.inventory.reserved");

        // Blog
        assertThat(BlogTopics.POST_LIKED).isEqualTo("blog.post.liked");
        assertThat(BlogTopics.POST_COMMENTED).isEqualTo("blog.post.commented");
        assertThat(BlogTopics.COMMENT_REPLIED).isEqualTo("blog.comment.replied");
        assertThat(BlogTopics.USER_FOLLOWED).isEqualTo("blog.user.followed");

        // Prism
        assertThat(PrismTopics.TASK_COMPLETED).isEqualTo("prism.task.completed");
        assertThat(PrismTopics.TASK_FAILED).isEqualTo("prism.task.failed");
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

        constructor.setAccessible(true);
        NotificationConstants instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
