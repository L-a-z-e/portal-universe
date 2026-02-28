package com.portal.universe.shoppingsellerservice.event;

import com.portal.universe.event.seller.CouponCreatedEvent;
import com.portal.universe.event.seller.CouponDeletedEvent;
import com.portal.universe.event.seller.CouponUpdatedEvent;
import com.portal.universe.event.seller.SellerTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventPublisher {

    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreated(CouponCreatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.COUPON_CREATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish CouponCreatedEvent: couponId={}, sellerId={}",
                                event.getCouponId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published CouponCreatedEvent: couponId={}, sellerId={}, offset={}",
                                event.getCouponId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUpdated(CouponUpdatedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.COUPON_UPDATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish CouponUpdatedEvent: couponId={}, sellerId={}",
                                event.getCouponId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published CouponUpdatedEvent: couponId={}, sellerId={}, offset={}",
                                event.getCouponId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleted(CouponDeletedEvent event) {
        String key = String.valueOf(event.getSellerId());
        avroKafkaTemplate.send(SellerTopics.COUPON_DELETED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish CouponDeletedEvent: couponId={}, sellerId={}",
                                event.getCouponId(), event.getSellerId(), ex);
                    } else {
                        log.info("Published CouponDeletedEvent: couponId={}, sellerId={}, offset={}",
                                event.getCouponId(), event.getSellerId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
