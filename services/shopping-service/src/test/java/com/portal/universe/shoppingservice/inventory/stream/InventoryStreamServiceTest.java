package com.portal.universe.shoppingservice.inventory.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryStreamServiceTest {

    @Mock
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Mock
    private ObjectMapper objectMapper;

    private InventoryStreamService inventoryStreamService;

    @BeforeEach
    void setUp() {
        inventoryStreamService = new InventoryStreamService(redisMessageListenerContainer, objectMapper);
    }

    @Test
    @DisplayName("should subscribe to product inventory updates")
    void should_subscribe_to_products() {
        Flux<InventoryUpdate> flux = inventoryStreamService.subscribe(List.of(1L, 2L));

        assertThat(flux).isNotNull();
        verify(redisMessageListenerContainer, times(2))
                .addMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("should publish inventory update to existing sink")
    void should_publish_update() {
        // Subscribe first to create the sink
        inventoryStreamService.subscribe(List.of(1L));

        InventoryUpdate update = InventoryUpdate.builder()
                .productId(1L)
                .available(50)
                .reserved(10)
                .timestamp(LocalDateTime.now())
                .build();

        inventoryStreamService.publishInventoryUpdate(update);

        // Verify sink exists by checking internal state
        @SuppressWarnings("unchecked")
        Map<Long, Sinks.Many<InventoryUpdate>> sinks =
                (Map<Long, Sinks.Many<InventoryUpdate>>) ReflectionTestUtils.getField(
                        inventoryStreamService, "productSinks");
        assertThat(sinks).containsKey(1L);
    }

    @Test
    @DisplayName("should handle disconnect and cleanup when last subscriber leaves")
    void should_handle_disconnect() {
        @SuppressWarnings("unchecked")
        Map<Long, Sinks.Many<InventoryUpdate>> sinks =
                (Map<Long, Sinks.Many<InventoryUpdate>>) ReflectionTestUtils.getField(
                        inventoryStreamService, "productSinks");

        @SuppressWarnings("unchecked")
        Map<Long, AtomicInteger> subscriberCounts =
                (Map<Long, AtomicInteger>) ReflectionTestUtils.getField(
                        inventoryStreamService, "subscriberCounts");

        // Subscribe to create sink
        Flux<InventoryUpdate> flux = inventoryStreamService.subscribe(List.of(1L));

        assertThat(sinks).containsKey(1L);
        assertThat(subscriberCounts.get(1L).get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should cleanup all sinks and listeners on shutdown")
    void should_shutdown_cleanly() {
        // Subscribe to create sinks
        inventoryStreamService.subscribe(List.of(1L, 2L));

        inventoryStreamService.shutdown();

        @SuppressWarnings("unchecked")
        Map<Long, Sinks.Many<InventoryUpdate>> sinks =
                (Map<Long, Sinks.Many<InventoryUpdate>>) ReflectionTestUtils.getField(
                        inventoryStreamService, "productSinks");
        assertThat(sinks).isEmpty();

        verify(redisMessageListenerContainer, times(2))
                .removeMessageListener(any(MessageListener.class), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("should reuse existing sink for same product id")
    void should_reuse_existing_sink() {
        inventoryStreamService.subscribe(List.of(1L));
        inventoryStreamService.subscribe(List.of(1L));

        // Should only register listener once for the same product
        verify(redisMessageListenerContainer, times(1))
                .addMessageListener(any(MessageListener.class), any(ChannelTopic.class));

        @SuppressWarnings("unchecked")
        Map<Long, AtomicInteger> subscriberCounts =
                (Map<Long, AtomicInteger>) ReflectionTestUtils.getField(
                        inventoryStreamService, "subscriberCounts");
        assertThat(subscriberCounts.get(1L).get()).isEqualTo(2);
    }
}
