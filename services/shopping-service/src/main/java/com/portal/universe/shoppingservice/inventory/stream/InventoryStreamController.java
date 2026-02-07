package com.portal.universe.shoppingservice.inventory.stream;

import com.portal.universe.commonlibrary.response.SseEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryStreamController {

    private final InventoryStreamService streamService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SseEnvelope<?>>> streamInventory(
            @RequestParam List<Long> productIds) {

        // Heartbeat every 30 seconds to keep connection alive
        Flux<ServerSentEvent<SseEnvelope<?>>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<SseEnvelope<?>>builder()
                        .comment("heartbeat")
                        .data(SseEnvelope.heartbeat())
                        .build());

        // Inventory updates
        Flux<ServerSentEvent<SseEnvelope<?>>> updates = streamService.subscribe(productIds)
                .map(update -> ServerSentEvent.<SseEnvelope<?>>builder()
                        .id(String.valueOf(update.getProductId()))
                        .event("inventory-update")
                        .data(SseEnvelope.of("inventory-update", update))
                        .build());

        return Flux.merge(heartbeat, updates);
    }
}
