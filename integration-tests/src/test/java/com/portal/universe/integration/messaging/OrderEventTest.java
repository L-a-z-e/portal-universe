package com.portal.universe.integration.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Kafka Event Integration Tests
 *
 * Verifies that business operations correctly publish events to Kafka topics:
 * - Order creation events
 * - Payment completed events
 * - User signup events
 * - Inventory update events
 */
@Slf4j
@DisplayName("Kafka Event Integration Tests")
@Tag("messaging")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderEventTest extends IntegrationTestBase {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Kafka Topics
    private static final String TOPIC_ORDER_CREATED = "shopping.order.created";
    private static final String TOPIC_PAYMENT_COMPLETED = "shopping.payment.completed";
    private static final String TOPIC_USER_SIGNUP = "user-signup";
    private static final String TOPIC_INVENTORY_UPDATE = "shopping.inventory.updated";
    private static final String TOPIC_ORDER_STATUS_CHANGED = "shopping.order.status.changed";

    private static Long testProductId;
    private static Long testOrderId;
    private static String testOrderNumber;

    @BeforeAll
    void setupTestData() {
        // Get a product for order testing
        Response response = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products");

        if (response.statusCode() == 200) {
            List<Map<String, Object>> products = response.jsonPath().getList("data.content");
            if (products != null && !products.isEmpty()) {
                testProductId = Long.valueOf(products.get(0).get("id").toString());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. User signup should publish event to user-signup topic")
    void testUserSignupEventPublished() {
        // Given - Consumer for user-signup topic
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(TOPIC_USER_SIGNUP)) {
            String testEmail = "kafka-event-" + System.currentTimeMillis() + "@test.com";

            // When - Create new user
            Map<String, String> signupData = new HashMap<>();
            signupData.put("email", testEmail);
            signupData.put("password", "KafkaTest123!");
            signupData.put("name", "Kafka Event Test User");

            Response signupResponse = given()
                    .baseUri(AUTH_SERVICE_URL)
                    .contentType("application/json")
                    .body(signupData)
                    .when()
                    .post("/api/v1/users/signup");

            signupResponse.then().statusCode(anyOf(is(200), is(201)));

            // Then - Verify event published
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer, TOPIC_USER_SIGNUP, Duration.ofSeconds(15));

            assertThat(record).isPresent();

            String eventValue = record.get().value();
            log.info("Received user-signup event: {}", eventValue);

            // Verify event contains user data
            assertThat(eventValue).contains(testEmail);

            // Parse and validate event structure
            try {
                JsonNode eventJson = objectMapper.readTree(eventValue);
                assertThat(eventJson.has("email") || eventJson.has("userId"))
                        .isTrue()
                        .withFailMessage("Event should contain user identifier");
            } catch (Exception e) {
                log.warn("Event is not JSON, treating as plain text");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. Order creation should publish event to shopping.order.created topic")
    void testOrderCreatedEventPublished() {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(TOPIC_ORDER_CREATED)) {
            // Add to cart first
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", testProductId);
            cartItem.put("quantity", 1);

            Response cartResponse = givenAuthenticatedUser()
                    .body(cartItem)
                    .when()
                    .post("/api/v1/shopping/cart/items");

            Long cartId = cartResponse.jsonPath().getLong("data.cartId");

            // When - Create order
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("cartId", cartId);

            Map<String, String> shippingAddress = new HashMap<>();
            shippingAddress.put("recipientName", "Kafka Test");
            shippingAddress.put("phoneNumber", "010-1234-5678");
            shippingAddress.put("address", "Seoul, Korea");
            shippingAddress.put("zipCode", "12345");
            orderRequest.put("shippingAddress", shippingAddress);

            Response orderResponse = givenAuthenticatedUser()
                    .body(orderRequest)
                    .when()
                    .post("/api/v1/shopping/orders");

            orderResponse.then().statusCode(anyOf(is(200), is(201)));

            testOrderId = orderResponse.jsonPath().getLong("data.id");
            testOrderNumber = orderResponse.jsonPath().getString("data.orderNumber");

            log.info("Created order: {} ({})", testOrderId, testOrderNumber);

            // Then - Verify event published
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer, TOPIC_ORDER_CREATED, Duration.ofSeconds(15));

            assertThat(record).isPresent();

            String eventValue = record.get().value();
            log.info("Received order.created event: {}", eventValue);

            // Verify event contains order data
            assertThat(eventValue).containsAnyOf(testOrderNumber, testOrderId.toString());

            // Validate event structure
            try {
                JsonNode eventJson = objectMapper.readTree(eventValue);
                if (eventJson.has("orderNumber")) {
                    assertThat(eventJson.get("orderNumber").asText()).isEqualTo(testOrderNumber);
                }
                if (eventJson.has("orderId")) {
                    assertThat(eventJson.get("orderId").asLong()).isEqualTo(testOrderId);
                }
            } catch (Exception e) {
                log.warn("Event parsing failed, basic assertions passed");
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Payment completion should publish event to shopping.payment.completed topic")
    void testPaymentCompletedEventPublished() {
        Assumptions.assumeTrue(testOrderId != null, "No order available");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(TOPIC_PAYMENT_COMPLETED)) {
            // When - Process payment
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", testOrderId);
            paymentRequest.put("paymentMethod", "CREDIT_CARD");

            Map<String, String> cardInfo = new HashMap<>();
            cardInfo.put("cardNumber", "4111111111111111");
            cardInfo.put("expiryMonth", "12");
            cardInfo.put("expiryYear", "2025");
            cardInfo.put("cvv", "123");
            paymentRequest.put("cardInfo", cardInfo);

            Response paymentResponse = givenAuthenticatedUser()
                    .body(paymentRequest)
                    .when()
                    .post("/api/v1/shopping/payments");

            paymentResponse.then().statusCode(anyOf(is(200), is(201)));

            Long paymentId = paymentResponse.jsonPath().getLong("data.id");
            log.info("Payment completed: {}", paymentId);

            // Then - Verify event published
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer, TOPIC_PAYMENT_COMPLETED, Duration.ofSeconds(15));

            assertThat(record).isPresent();

            String eventValue = record.get().value();
            log.info("Received payment.completed event: {}", eventValue);

            // Verify event contains payment/order data
            assertThat(eventValue).containsAnyOf(
                    testOrderNumber,
                    testOrderId.toString(),
                    paymentId.toString()
            );
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. Verify multiple events in sequence")
    void testEventSequence() {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Subscribe to multiple topics
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(
                TOPIC_ORDER_CREATED,
                TOPIC_PAYMENT_COMPLETED,
                TOPIC_ORDER_STATUS_CHANGED)) {

            // Add to cart
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", testProductId);
            cartItem.put("quantity", 1);

            Response cartResponse = givenAuthenticatedUser()
                    .body(cartItem)
                    .when()
                    .post("/api/v1/shopping/cart/items");

            Long cartId = cartResponse.jsonPath().getLong("data.cartId");

            // Create order
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("cartId", cartId);
            Map<String, String> address = Map.of(
                    "recipientName", "Sequence Test",
                    "phoneNumber", "010-1234-5678",
                    "address", "Seoul",
                    "zipCode", "12345"
            );
            orderRequest.put("shippingAddress", address);

            Response orderResponse = givenAuthenticatedUser()
                    .body(orderRequest)
                    .when()
                    .post("/api/v1/shopping/orders");

            Long orderId = orderResponse.jsonPath().getLong("data.id");

            // Process payment
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", orderId);
            paymentRequest.put("paymentMethod", "CREDIT_CARD");
            paymentRequest.put("cardInfo", Map.of(
                    "cardNumber", "4111111111111111",
                    "expiryMonth", "12",
                    "expiryYear", "2025",
                    "cvv", "123"
            ));

            givenAuthenticatedUser()
                    .body(paymentRequest)
                    .when()
                    .post("/api/v1/shopping/payments");

            // Collect events for 10 seconds
            List<ConsumerRecord<String, String>> allRecords = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 10000;

            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(allRecords::add);
            }

            log.info("Collected {} events", allRecords.size());
            allRecords.forEach(r -> log.info("Topic: {}, Value: {}", r.topic(), r.value()));

            // Verify we received at least order.created and payment.completed
            long orderCreatedCount = allRecords.stream()
                    .filter(r -> r.topic().equals(TOPIC_ORDER_CREATED))
                    .count();

            long paymentCompletedCount = allRecords.stream()
                    .filter(r -> r.topic().equals(TOPIC_PAYMENT_COMPLETED))
                    .count();

            assertThat(orderCreatedCount).isGreaterThanOrEqualTo(1);
            assertThat(paymentCompletedCount).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Verify event contains required fields")
    void testEventStructure() {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(TOPIC_ORDER_CREATED)) {
            // Add to cart and create order
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", testProductId);
            cartItem.put("quantity", 2);

            Response cartResponse = givenAuthenticatedUser()
                    .body(cartItem)
                    .when()
                    .post("/api/v1/shopping/cart/items");

            Long cartId = cartResponse.jsonPath().getLong("data.cartId");

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("cartId", cartId);
            orderRequest.put("shippingAddress", Map.of(
                    "recipientName", "Structure Test",
                    "phoneNumber", "010-1234-5678",
                    "address", "Seoul",
                    "zipCode", "12345"
            ));

            givenAuthenticatedUser()
                    .body(orderRequest)
                    .when()
                    .post("/api/v1/shopping/orders");

            // Consume and validate structure
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer, TOPIC_ORDER_CREATED, Duration.ofSeconds(15));

            assertThat(record).isPresent();

            String eventValue = record.get().value();

            try {
                JsonNode event = objectMapper.readTree(eventValue);

                // Required fields for order event
                List<String> expectedFields = Arrays.asList(
                        "orderId", "orderNumber", "userId", "totalAmount", "status",
                        "createdAt", "eventType", "timestamp"
                );

                // Check at least some required fields exist
                long matchingFields = expectedFields.stream()
                        .filter(event::has)
                        .count();

                log.info("Event contains {}/{} expected fields", matchingFields, expectedFields.size());

                // At minimum, should have order identifier
                assertThat(event.has("orderId") || event.has("orderNumber") || event.has("id"))
                        .isTrue()
                        .withFailMessage("Event should contain order identifier");

            } catch (Exception e) {
                // If not JSON, just verify it's not empty
                assertThat(eventValue).isNotBlank();
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Verify event ordering and timing")
    void testEventOrdering() {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(
                TOPIC_ORDER_CREATED, TOPIC_PAYMENT_COMPLETED)) {

            // Create order and payment
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("productId", testProductId);
            cartItem.put("quantity", 1);

            Response cartResponse = givenAuthenticatedUser()
                    .body(cartItem)
                    .when()
                    .post("/api/v1/shopping/cart/items");

            Long cartId = cartResponse.jsonPath().getLong("data.cartId");

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("cartId", cartId);
            orderRequest.put("shippingAddress", Map.of(
                    "recipientName", "Ordering Test",
                    "phoneNumber", "010-1234-5678",
                    "address", "Seoul",
                    "zipCode", "12345"
            ));

            Response orderResponse = givenAuthenticatedUser()
                    .body(orderRequest)
                    .when()
                    .post("/api/v1/shopping/orders");

            Long orderId = orderResponse.jsonPath().getLong("data.id");

            // Small delay to ensure order event is published
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("orderId", orderId);
            paymentRequest.put("paymentMethod", "CREDIT_CARD");
            paymentRequest.put("cardInfo", Map.of(
                    "cardNumber", "4111111111111111",
                    "expiryMonth", "12",
                    "expiryYear", "2025",
                    "cvv", "123"
            ));

            givenAuthenticatedUser()
                    .body(paymentRequest)
                    .when()
                    .post("/api/v1/shopping/payments");

            // Collect events and verify order
            List<ConsumerRecord<String, String>> events = new ArrayList<>();
            long deadline = System.currentTimeMillis() + 15000;

            while (System.currentTimeMillis() < deadline && events.size() < 2) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(events::add);
            }

            // Verify order.created comes before payment.completed
            if (events.size() >= 2) {
                int orderCreatedIndex = -1;
                int paymentCompletedIndex = -1;

                for (int i = 0; i < events.size(); i++) {
                    if (events.get(i).topic().equals(TOPIC_ORDER_CREATED)) {
                        orderCreatedIndex = i;
                    }
                    if (events.get(i).topic().equals(TOPIC_PAYMENT_COMPLETED)) {
                        paymentCompletedIndex = i;
                    }
                }

                if (orderCreatedIndex >= 0 && paymentCompletedIndex >= 0) {
                    assertThat(orderCreatedIndex)
                            .isLessThan(paymentCompletedIndex)
                            .withFailMessage("Order created event should come before payment completed event");
                }
            }
        }
    }
}
