package com.portal.universe.authservice.follow.event;

import com.portal.universe.authservice.common.event.ResilientKafkaPublisher;
import com.portal.universe.event.blog.BlogTopics;
import com.portal.universe.event.blog.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFollowedKafkaPublisher {

    private final ResilientKafkaPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserFollowedEvent event) {
        log.info("Publishing user followed event: followeeId={}, followerId={}",
                event.getFolloweeId(), event.getFollowerId());
        kafkaPublisher.send(BlogTopics.USER_FOLLOWED, event.getFolloweeId().toString(), event);
    }
}
