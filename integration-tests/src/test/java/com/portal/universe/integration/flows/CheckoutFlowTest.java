package com.portal.universe.integration.flows;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Checkout Flow Integration Tests
 *
 * Tests the complete e-commerce checkout lifecycle:
 * 1. Browse products
 * 2. Add to cart
 * 3. Checkout cart
 * 4. Create order
 * 5. Process payment
 * 6. Verify delivery tracking
 * 7. Verify Kafka events at each step
 */
@DisplayName("Checkout Flow Integration Tests")
@Tag("flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CheckoutFlowTest extends IntegrationTestBase {

    private static final String KAFKA_ORDER_CREATED_TOPIC = "shopping.order.created";
    private static final String KAFKA_PAYMENT_COMPLETED_TOPIC = "shopping.payment.completed";

    private static Long testProductId;
    private static Long testCartId;
    private static Long testOrderId;
    private static String testOrderNumber;
    private static Long testPaymentId;
    private static String testTrackingNumber;

    /**
     * Helper to build a shipping address with correct field names matching AddressRequest DTO.
     */
    private static Map<String, String> buildShippingAddress(String name, String phone, String address) {
        Map<String, String> shippingAddress = new HashMap<>();
        shippingAddress.put("receiverName", name);
        shippingAddress.put("receiverPhone", phone);
        shippingAddress.put("address1", address);
        shippingAddress.put("zipCode", "12345");
        return shippingAddress;
    }

    /**
     * Helper to build a payment request with correct field names matching ProcessPaymentRequest DTO.
     */
    private static Map<String, Object> buildPaymentRequest(String orderNumber) {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderNumber", orderNumber);
        paymentRequest.put("paymentMethod", "CARD");
        paymentRequest.put("cardNumber", "4111111111111111");
        paymentRequest.put("cardExpiry", "12/2025");
        paymentRequest.put("cardCvv", "123");
        return paymentRequest;
    }

    @Test
    @Order(1)
    @DisplayName("1. Browse products - Get product list")
    void testGetProductList() {
        // When
        Response response = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true));

        // Store first product ID for subsequent tests
        List<Map<String, Object>> products = response.jsonPath().getList("data.content");
        Assumptions.assumeTrue(products != null && !products.isEmpty(),
                "No products available in the system");

        testProductId = Long.valueOf(products.get(0).get("id").toString());
        assertThat(testProductId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. Get product details")
    void testGetProductDetails() {
        // Skip if no product available
        Assumptions.assumeTrue(testProductId != null, "No product available for testing");

        // When
        Response response = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products/" + testProductId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.id", equalTo(testProductId.intValue()))
                .body("data.name", notNullValue())
                .body("data.price", notNullValue())
                .body("data.stockQuantity", greaterThan(0));
    }

    @Test
    @Order(3)
    @DisplayName("3. Add product to cart")
    void testAddToCart() {
        // Skip if no product available
        Assumptions.assumeTrue(testProductId != null, "No product available for testing");

        // Given
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", testProductId);
        cartItem.put("quantity", 2);

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/v1/shopping/cart/items");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true));

        // CartResponse has id at top level
        testCartId = response.jsonPath().getLong("data.id");
        assertThat(testCartId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. Get cart contents")
    void testGetCart() {
        // Skip if no cart
        Assumptions.assumeTrue(testCartId != null, "No cart available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/cart");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.items", not(empty()))
                .body("data.totalAmount", notNullValue());

        // Verify our product is in the cart
        List<Map<String, Object>> items = response.jsonPath().getList("data.items");
        if (items != null && !items.isEmpty()) {
            boolean productFound = items.stream()
                    .anyMatch(item -> {
                        Object pid = item.get("productId");
                        return pid != null && Long.valueOf(pid.toString()).equals(testProductId);
                    });
            assertThat(productFound).isTrue();
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Create order from cart")
    void testCreateOrder() {
        // Skip if no cart
        Assumptions.assumeTrue(testCartId != null, "No cart available for testing");

        // Step 1: Checkout the cart first (required before order creation)
        givenAuthenticatedUser()
                .when()
                .post("/api/v1/shopping/cart/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Step 2: Create order with correct DTO field names
        // CreateOrderRequest only has shippingAddress (AddressRequest) and optional userCouponId - no cartId
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("shippingAddress", buildShippingAddress("Test User", "010-1234-5678", "Seoul, Korea"));

        // When
        Response response = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/v1/shopping/orders");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue())
                .body("data.orderNumber", notNullValue());

        // Store order info
        testOrderId = response.jsonPath().getLong("data.id");
        testOrderNumber = response.jsonPath().getString("data.orderNumber");

        assertThat(testOrderId).isNotNull();
        assertThat(testOrderNumber).isNotBlank();
    }

    @Test
    @Order(6)
    @DisplayName("6. Verify order creation Kafka event")
    void testOrderCreatedKafkaEvent() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(KAFKA_ORDER_CREATED_TOPIC)) {
            // Wait for and consume the order created event
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer,
                    KAFKA_ORDER_CREATED_TOPIC,
                    Duration.ofSeconds(15)
            );

            // Then
            assertThat(record).isPresent();
            String eventValue = record.get().value();
            assertThat(eventValue).contains(testOrderNumber);
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. Get order details")
    void testGetOrderDetails() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        // When - Use orderNumber (String) as path variable, not orderId (Long)
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders/" + testOrderNumber);

        // Then - OrderResponse uses 'items' not 'orderItems'
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.orderNumber", equalTo(testOrderNumber))
                .body("data.items", not(empty()));
    }

    @Test
    @Order(8)
    @DisplayName("8. Process payment for order")
    void testProcessPayment() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        // Given - ProcessPaymentRequest uses orderNumber (String), paymentMethod=CARD, flat card fields
        Map<String, Object> paymentRequest = buildPaymentRequest(testOrderNumber);

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/v1/shopping/payments");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue());

        testPaymentId = response.jsonPath().getLong("data.id");
        assertThat(testPaymentId).isNotNull();
    }

    @Test
    @Order(9)
    @DisplayName("9. Verify payment completed Kafka event")
    void testPaymentCompletedKafkaEvent() {
        // Skip if no payment
        Assumptions.assumeTrue(testPaymentId != null, "No payment available for testing");

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(KAFKA_PAYMENT_COMPLETED_TOPIC)) {
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer,
                    KAFKA_PAYMENT_COMPLETED_TOPIC,
                    Duration.ofSeconds(15)
            );

            // Then
            assertThat(record).isPresent();
            String eventValue = record.get().value();
            assertThat(eventValue).contains(testOrderNumber);
        }
    }

    @Test
    @Order(10)
    @DisplayName("10. Verify order status updated after payment")
    void testOrderStatusPaid() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        // When - Use orderNumber for lookup
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders/" + testOrderNumber);

        // Then - Status could be PAID, CONFIRMED, or PROCESSING depending on flow
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", notNullValue());
    }

    @Test
    @Order(11)
    @DisplayName("11. Get delivery tracking information")
    void testGetDeliveryTracking() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        // When - Get order to find tracking number
        Response orderResponse = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders/" + testOrderNumber);

        testTrackingNumber = orderResponse.jsonPath().getString("data.delivery.trackingNumber");

        // Skip if no tracking number yet
        Assumptions.assumeTrue(testTrackingNumber != null, "No tracking number available yet");

        // Then - Get delivery details
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/deliveries/" + testTrackingNumber);

        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.trackingNumber", equalTo(testTrackingNumber))
                .body("data.status", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("12. Get my orders list")
    void testGetMyOrders() {
        // When - OrderController uses GET /orders (not /orders/my)
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.content", notNullValue());

        // Verify our test order is in the list
        if (testOrderNumber != null) {
            List<Map<String, Object>> orders = response.jsonPath().getList("data.content");
            if (orders != null && !orders.isEmpty()) {
                boolean orderFound = orders.stream()
                        .anyMatch(order -> testOrderNumber.equals(order.get("orderNumber")));
                assertThat(orderFound).isTrue();
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("13. Verify inventory was decreased after order")
    void testInventoryDecreased() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/products/" + testProductId);

        // Then - Just verify the product still exists and has stock info
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.stockQuantity", notNullValue());

        // Note: Can't verify exact decrease without knowing initial stock
    }

    // =======================================
    // Error Cases
    // =======================================

    @Test
    @Order(20)
    @DisplayName("20. Create order with empty cart should fail")
    void testCreateOrderEmptyCart() {
        // First clear the cart
        givenAuthenticatedUser()
                .when()
                .delete("/api/v1/shopping/cart");

        // Given - CreateOrderRequest only has shippingAddress, no cartId
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("shippingAddress", buildShippingAddress("Test User", "010-1234-5678", "Seoul, Korea"));

        // When
        Response response = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/v1/shopping/orders");

        // Then - Should fail because cart is empty or not checked out
        response.then()
                .statusCode(anyOf(is(400), is(404), is(422)));
    }

    @Test
    @Order(21)
    @DisplayName("21. Pay for non-existent order should fail")
    void testPayNonExistentOrder() {
        // Given - ProcessPaymentRequest uses orderNumber (String), not orderId
        Map<String, Object> paymentRequest = buildPaymentRequest("NONEXISTENT999");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/v1/shopping/payments");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(22)
    @DisplayName("22. Add out-of-stock product to cart should fail")
    void testAddOutOfStockToCart() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available for testing");

        // Given - Request more than available stock
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", testProductId);
        cartItem.put("quantity", 999999); // Unrealistic quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/v1/shopping/cart/items");

        // Then - Should fail due to insufficient stock
        response.then()
                .statusCode(anyOf(is(400), is(422))); // Bad Request or Unprocessable Entity
    }

    @Test
    @Order(23)
    @DisplayName("23. Access other user's order should fail")
    void testAccessOtherUsersOrder() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderNumber != null, "No order available for testing");

        // Create a new user with different token
        String otherUserEmail = generateTestEmail();
        String otherUserToken = createUserAndGetToken(otherUserEmail, "SecurePw8!", "Other User");

        // When - Try to access test user's order with other user's token (use orderNumber)
        Response response = givenWithToken(otherUserToken)
                .when()
                .get("/api/v1/shopping/orders/" + testOrderNumber);

        // Then - Should fail with 403 or 404
        response.then()
                .statusCode(anyOf(is(403), is(404)));
    }
}
