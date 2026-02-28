package com.portal.universe.shoppingservice.event;

import com.portal.universe.event.shopping.*;
import com.portal.universe.shoppingservice.event.outbox.OutboxEvent;
import com.portal.universe.shoppingservice.event.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    public void publishOrderCreated(OrderCreatedEvent event) {
        saveOutbox(ShoppingTopics.ORDER_CREATED, event.getOrderNumber().toString(), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        saveOutbox(ShoppingTopics.ORDER_CONFIRMED, event.getOrderNumber().toString(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        saveOutbox(ShoppingTopics.ORDER_CANCELLED, event.getOrderNumber().toString(), event);
    }

    public void publishInventoryReserved(InventoryReservedEvent event) {
        saveOutbox(ShoppingTopics.INVENTORY_RESERVED, event.getOrderNumber().toString(), event);
    }

    public void publishDeliveryShipped(DeliveryShippedEvent event) {
        saveOutbox(ShoppingTopics.DELIVERY_SHIPPED, event.getTrackingNumber().toString(), event);
    }

    public void publishCouponIssued(CouponIssuedEvent event) {
        saveOutbox(ShoppingTopics.COUPON_ISSUED, event.getCouponCode().toString(), event);
    }

    public void publishTimeDealStarted(TimeDealStartedEvent event) {
        saveOutbox(ShoppingTopics.TIMEDEAL_STARTED, String.valueOf(event.getTimeDealId()), event);
    }

    public void publishOrderSettlementCreated(OrderSettlementCreatedEvent event) {
        saveOutbox(ShoppingTopics.ORDER_SETTLEMENT_CREATED, event.getOrderNumber().toString(), event);
    }

    private void saveOutbox(String topic, String key, SpecificRecord event) {
        outboxEventRepository.save(OutboxEvent.create(topic, key, event));
    }
}
