package com.portal.universe.shoppingservice.inventory.stream;

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
    public Flux<ServerSentEvent<InventoryUpdate>> streamInventory(
            @RequestParam List<Long> productIds) {

        // Heartbeat every 30 seconds to keep connection alive
        Flux<ServerSentEvent<InventoryUpdate>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<InventoryUpdate>builder()
                        .comment("heartbeat")
                        .build());

        // Inventory updates
        Flux<ServerSentEvent<InventoryUpdate>> updates = streamService.subscribe(productIds)
                .map(update -> ServerSentEvent.<InventoryUpdate>builder()
                        .id(String.valueOf(update.getProductId()))
                        .event("inventory-update")
                        .data(update)
                        .build());

        return Flux.merge(heartbeat, updates);
    }
}
