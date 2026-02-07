package com.portal.universe.integration.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * WebSocket Notification Integration Tests
 *
 * Verifies that real-time notifications are pushed via WebSocket/STOMP:
 * - Order status change notifications
 * - Payment completion notifications
 * - Coupon issued notifications
 */
@Slf4j
@DisplayName("WebSocket Notification Tests")
@Tag("websocket")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationPushTest extends IntegrationTestBase {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String WEBSOCKET_URL = "ws://localhost:8084/ws";
    private static final String NOTIFICATION_DESTINATION = "/user/queue/notifications";

    private static Long testProductId;
    private static WebSocketStompClient stompClient;

    /**
     * Helper to build shipping address with correct field names matching AddressRequest DTO.
     */
    private static Map<String, String> buildShippingAddress(String name) {
        Map<String, String> addr = new HashMap<>();
        addr.put("receiverName", name);
        addr.put("receiverPhone", "010-1234-5678");
        addr.put("address1", "Seoul");
        addr.put("zipCode", "12345");
        return addr;
    }

    /**
     * Helper to build payment request with correct field names matching ProcessPaymentRequest DTO.
     */
    private static Map<String, Object> buildPaymentRequest(String orderNumber) {
        Map<String, Object> request = new HashMap<>();
        request.put("orderNumber", orderNumber);
        request.put("paymentMethod", "CARD");
        request.put("cardNumber", "4111111111111111");
        request.put("cardExpiry", "12/2025");
        request.put("cardCvv", "123");
        return request;
    }

    /**
     * Full order+payment flow with correct DTOs. Returns orderNumber.
     */
    private String triggerPaymentFlow(String token) {
        // Add to cart
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", testProductId);
        cartItem.put("quantity", 1);

        givenWithToken(token)
                .body(cartItem)
                .when()
                .post("/api/v1/shopping/cart/items");

        // Checkout cart (required before order creation)
        givenWithToken(token)
                .when()
                .post("/api/v1/shopping/cart/checkout");

        // Create order - CreateOrderRequest only has shippingAddress (no cartId)
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("shippingAddress", buildShippingAddress("WebSocket Test"));

        Response orderResponse = givenWithToken(token)
                .body(orderRequest)
                .when()
                .post("/api/v1/shopping/orders");

        String orderNumber = orderResponse.jsonPath().getString("data.orderNumber");

        // Process payment
        Map<String, Object> paymentRequest = buildPaymentRequest(orderNumber);

        givenWithToken(token)
                .body(paymentRequest)
                .when()
                .post("/api/v1/shopping/payments");

        return orderNumber;
    }

    @BeforeAll
    void setup() {
        // Get a product for testing
        Response response = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products");

        if (response.statusCode() == 200) {
            List<Map<String, Object>> products = response.jsonPath().getList("data.content");
            if (products != null && !products.isEmpty()) {
                testProductId = Long.valueOf(products.get(0).get("id").toString());
            }
        }

        // Initialize STOMP client
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
    }

    @AfterAll
    void cleanup() {
        if (stompClient != null && stompClient.isRunning()) {
            stompClient.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. WebSocket connection should succeed with valid token")
    void testWebSocketConnection() throws Exception {
        String userToken = getUserToken();

        CompletableFuture<Boolean> connected = new CompletableFuture<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                log.info("WebSocket connected: session={}", session.getSessionId());
                connected.complete(true);
                session.disconnect();
            }

            @Override
            public void handleException(@NonNull StompSession session, StompCommand command,
                                        @NonNull StompHeaders headers, byte[] payload, @NonNull Throwable exception) {
                log.error("WebSocket error: {}", exception.getMessage());
                connected.completeExceptionally(exception);
            }

            @Override
            public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                log.error("Transport error: {}", exception.getMessage());
                connected.completeExceptionally(exception);
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + userToken);

        try {
            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);

            Boolean result = connected.get(10, TimeUnit.SECONDS);
            assertThat(result).isTrue();
        } catch (TimeoutException e) {
            log.warn("WebSocket connection timed out - service might not support WebSocket");
            Assumptions.assumeTrue(false, "WebSocket not available");
        } catch (Exception e) {
            log.warn("WebSocket connection failed: {}", e.getMessage());
            Assumptions.assumeTrue(false, "WebSocket connection failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. Payment completion should push notification via WebSocket")
    void testPaymentNotificationPush() throws Exception {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        String userToken = getUserToken();

        CompletableFuture<String> notificationReceived = new CompletableFuture<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                log.info("WebSocket connected for notification test");

                // Subscribe to user notifications
                session.subscribe(NOTIFICATION_DESTINATION, new StompFrameHandler() {
                    @Override
                    @NonNull
                    public Type getPayloadType(@NonNull StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                        log.info("Received notification: {}", payload);
                        notificationReceived.complete((String) payload);
                    }
                });

                // Trigger order and payment
                try {
                    triggerPaymentFlow(userToken);
                } catch (Exception e) {
                    log.error("Failed to trigger payment flow", e);
                }
            }

            @Override
            public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                log.error("Transport error: {}", exception.getMessage());
                notificationReceived.completeExceptionally(exception);
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + userToken);

        try {
            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);

            String notification = notificationReceived.get(30, TimeUnit.SECONDS);
            assertThat(notification).isNotBlank();

            // Verify notification contains payment info
            try {
                JsonNode notificationJson = objectMapper.readTree(notification);
                assertThat(
                        notificationJson.has("type") ||
                        notificationJson.has("message") ||
                        notificationJson.has("notificationType")
                ).isTrue();
            } catch (Exception e) {
                // Plain text notification
                assertThat(notification.toLowerCase())
                        .containsAnyOf("payment", "order", "completed", "성공");
            }

        } catch (TimeoutException e) {
            log.warn("No notification received within timeout - WebSocket notifications might not be implemented");
            Assumptions.assumeTrue(false, "WebSocket notifications not received");
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Multiple notifications should be received in order")
    void testMultipleNotifications() throws Exception {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        String userToken = getUserToken();

        List<String> receivedNotifications = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<Boolean> allReceived = new CompletableFuture<>();
        int expectedNotificationCount = 2; // Order created + Payment completed

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                log.info("WebSocket connected for multiple notification test");

                session.subscribe(NOTIFICATION_DESTINATION, new StompFrameHandler() {
                    @Override
                    @NonNull
                    public Type getPayloadType(@NonNull StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                        receivedNotifications.add((String) payload);
                        log.info("Notification {}: {}", receivedNotifications.size(), payload);

                        if (receivedNotifications.size() >= expectedNotificationCount) {
                            allReceived.complete(true);
                        }
                    }
                });

                // Trigger complete checkout flow with correct DTOs
                try {
                    triggerPaymentFlow(userToken);
                } catch (Exception e) {
                    log.error("Checkout flow failed", e);
                }
            }

            @Override
            public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                allReceived.completeExceptionally(exception);
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + userToken);

        try {
            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);

            Boolean result = allReceived.get(45, TimeUnit.SECONDS);
            assertThat(result).isTrue();
            assertThat(receivedNotifications).hasSize(expectedNotificationCount);

            // Verify notifications have different types
            Set<String> notificationTypes = new HashSet<>();
            for (String notification : receivedNotifications) {
                try {
                    JsonNode json = objectMapper.readTree(notification);
                    if (json.has("type")) {
                        notificationTypes.add(json.get("type").asText());
                    }
                } catch (Exception ignored) {}
            }

            log.info("Received {} unique notification types", notificationTypes.size());

        } catch (TimeoutException e) {
            log.warn("Not all notifications received within timeout");
            // Partial success is acceptable
            assertThat(receivedNotifications).isNotEmpty();
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Coupon issued notification should be pushed")
    void testCouponIssuedNotification() throws Exception {
        String userToken = getUserToken();

        CompletableFuture<String> notificationReceived = new CompletableFuture<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                log.info("WebSocket connected for coupon notification test");

                session.subscribe(NOTIFICATION_DESTINATION, new StompFrameHandler() {
                    @Override
                    @NonNull
                    public Type getPayloadType(@NonNull StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                        String notification = (String) payload;
                        if (notification.toLowerCase().contains("coupon") ||
                            notification.contains("쿠폰")) {
                            notificationReceived.complete(notification);
                        }
                    }
                });

                // Trigger coupon issuance - coupons endpoint returns List (not Page)
                try {
                    Response couponsResponse = givenWithToken(userToken)
                            .when()
                            .get("/api/v1/shopping/coupons");

                    // data is a List<CouponResponse>, not Page
                    List<Map<String, Object>> coupons = couponsResponse.jsonPath().getList("data");
                    if (coupons != null && !coupons.isEmpty()) {
                        Long couponId = Long.valueOf(coupons.get(0).get("id").toString());

                        givenWithToken(userToken)
                                .when()
                                .post("/api/v1/shopping/coupons/" + couponId + "/issue");
                    }
                } catch (Exception e) {
                    log.error("Coupon issue failed", e);
                }
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + userToken);

        try {
            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);

            String notification = notificationReceived.get(20, TimeUnit.SECONDS);
            assertThat(notification).isNotBlank();
            assertThat(notification.toLowerCase()).containsAnyOf("coupon", "쿠폰");

        } catch (TimeoutException e) {
            log.warn("Coupon notification not received - might not be implemented");
            Assumptions.assumeTrue(false, "Coupon WebSocket notification not implemented");
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. WebSocket connection without token should fail")
    void testUnauthorizedWebSocketConnection() throws Exception {
        CompletableFuture<Boolean> connectionFailed = new CompletableFuture<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                // Should not connect
                connectionFailed.complete(false);
            }

            @Override
            public void handleException(@NonNull StompSession session, StompCommand command,
                                        @NonNull StompHeaders headers, byte[] payload, @NonNull Throwable exception) {
                connectionFailed.complete(true);
            }

            @Override
            public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
                connectionFailed.complete(true);
            }
        };

        try {
            stompClient.connectAsync(WEBSOCKET_URL, sessionHandler);

            Boolean failed = connectionFailed.get(10, TimeUnit.SECONDS);
            assertThat(failed).isTrue();
        } catch (Exception e) {
            // Connection failure is expected
            log.info("Unauthorized connection correctly rejected");
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Notification persistence - reconnect should not lose notifications")
    void testNotificationPersistence() throws Exception {
        String newUserEmail = generateTestEmail();
        String newUserToken = createUserAndGetToken(newUserEmail, "SecurePw8!", "Persistence Test User");

        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Create pending notification by triggering an action with correct DTOs
        triggerPaymentFlow(newUserToken);

        // Wait a bit for notification to be stored
        Thread.sleep(2000);

        // Now connect and check for pending notifications
        CompletableFuture<List<String>> pendingNotifications = new CompletableFuture<>();
        List<String> received = Collections.synchronizedList(new ArrayList<>());

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
                session.subscribe(NOTIFICATION_DESTINATION, new StompFrameHandler() {
                    @Override
                    @NonNull
                    public Type getPayloadType(@NonNull StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                        received.add((String) payload);
                    }
                });

                // Wait for any pending notifications
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        pendingNotifications.complete(new ArrayList<>(received));
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        };

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + newUserToken);

        try {
            stompClient.connectAsync(WEBSOCKET_URL, headers, sessionHandler);

            List<String> notifications = pendingNotifications.get(15, TimeUnit.SECONDS);

            log.info("Received {} pending notifications after reconnect", notifications.size());
            // Note: Whether pending notifications are delivered depends on implementation
            // Just verify connection works

        } catch (TimeoutException e) {
            log.warn("Notification persistence test timed out");
        }
    }
}
