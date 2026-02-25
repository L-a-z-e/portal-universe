package com.portal.universe.shoppingservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventBridgePublisher {

    private static final String EVENT_BUS_NAME = "portal-universe";
    private static final String SOURCE = "portal-universe.shopping-service";

    private final EventBridgeClient eventBridgeClient;
    private final ObjectMapper objectMapper;

    @Async
    public void publishOrderCreated(String orderNumber, String userId,
                                     BigDecimal totalAmount, int itemCount,
                                     List<Map<String, Object>> items) {
        try {
            Map<String, Object> detail = Map.of(
                    "orderNumber", orderNumber,
                    "userId", userId,
                    "totalAmount", totalAmount.intValue(),
                    "itemCount", itemCount,
                    "items", items
            );

            String detailJson = objectMapper.writeValueAsString(detail);

            PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                    .eventBusName(EVENT_BUS_NAME)
                    .source(SOURCE)
                    .detailType("OrderCreated")
                    .detail(detailJson)
                    .build();

            PutEventsResponse response = eventBridgeClient.putEvents(
                    PutEventsRequest.builder().entries(entry).build());

            for (PutEventsResultEntry resultEntry : response.entries()) {
                if (resultEntry.errorCode() != null) {
                    log.warn("EventBridge publish failed: code={}, message={}",
                            resultEntry.errorCode(), resultEntry.errorMessage());
                } else {
                    log.info("EventBridge event published: eventId={}, order={}",
                            resultEntry.eventId(), orderNumber);
                }
            }
        } catch (Exception e) {
            log.warn("EventBridge publish error (non-blocking): order={}, error={}",
                    orderNumber, e.getMessage());
        }
    }
}
