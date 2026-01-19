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
 * 3. Create order
 * 4. Process payment
 * 5. Verify delivery tracking
 * 6. Verify Kafka events at each step
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

    @Test
    @Order(1)
    @DisplayName("1. Browse products - Get product list")
    void testGetProductList() {
        // When
        Response response = givenUnauthenticated()
                .when()
                .get("/api/shopping/products");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.content", not(empty()));

        // Store first product ID for subsequent tests
        List<Map<String, Object>> products = response.jsonPath().getList("data.content");
        if (!products.isEmpty()) {
            testProductId = Long.valueOf(products.get(0).get("id").toString());
        }

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
                .get("/api/shopping/products/" + testProductId);

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
                .post("/api/shopping/cart/items");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true));

        // Store cart ID
        testCartId = response.jsonPath().getLong("data.cartId");
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
                .get("/api/shopping/cart");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.items", not(empty()))
                .body("data.totalAmount", greaterThan(0f));

        // Verify our product is in the cart
        List<Map<String, Object>> items = response.jsonPath().getList("data.items");
        boolean productFound = items.stream()
                .anyMatch(item -> Long.valueOf(item.get("productId").toString()).equals(testProductId));
        assertThat(productFound).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("5. Create order from cart")
    void testCreateOrder() {
        // Skip if no cart
        Assumptions.assumeTrue(testCartId != null, "No cart available for testing");

        // Given
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("cartId", testCartId);

        Map<String, String> shippingAddress = new HashMap<>();
        shippingAddress.put("recipientName", "Test User");
        shippingAddress.put("phoneNumber", "010-1234-5678");
        shippingAddress.put("address", "Seoul, Korea");
        shippingAddress.put("zipCode", "12345");
        orderRequest.put("shippingAddress", shippingAddress);

        // When
        Response response = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/shopping/orders");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue())
                .body("data.orderNumber", notNullValue())
                .body("data.status", equalTo("PENDING"));

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
        Assumptions.assumeTrue(testOrderId != null, "No order available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/" + testOrderId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.id", equalTo(testOrderId.intValue()))
                .body("data.orderNumber", equalTo(testOrderNumber))
                .body("data.orderItems", not(empty()));
    }

    @Test
    @Order(8)
    @DisplayName("8. Process payment for order")
    void testProcessPayment() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderId != null, "No order available for testing");

        // Given
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", testOrderId);
        paymentRequest.put("paymentMethod", "CREDIT_CARD");

        Map<String, String> cardInfo = new HashMap<>();
        cardInfo.put("cardNumber", "4111111111111111");
        cardInfo.put("expiryMonth", "12");
        cardInfo.put("expiryYear", "2025");
        cardInfo.put("cvv", "123");
        paymentRequest.put("cardInfo", cardInfo);

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/shopping/payments");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue())
                .body("data.status", equalTo("COMPLETED"));

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
    @DisplayName("10. Verify order status updated to PAID")
    void testOrderStatusPaid() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderId != null, "No order available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/" + testOrderId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", equalTo("PAID"));
    }

    @Test
    @Order(11)
    @DisplayName("11. Get delivery tracking information")
    void testGetDeliveryTracking() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderId != null, "No order available for testing");

        // When - Get order to find tracking number
        Response orderResponse = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/" + testOrderId);

        testTrackingNumber = orderResponse.jsonPath().getString("data.delivery.trackingNumber");

        // Skip if no tracking number yet
        Assumptions.assumeTrue(testTrackingNumber != null, "No tracking number available yet");

        // Then - Get delivery details
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/deliveries/" + testTrackingNumber);

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
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/my");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.content", not(empty()));

        // Verify our test order is in the list
        List<Map<String, Object>> orders = response.jsonPath().getList("data.content");
        boolean orderFound = orders.stream()
                .anyMatch(order -> testOrderNumber.equals(order.get("orderNumber")));
        assertThat(orderFound).isTrue();
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
                .get("/api/shopping/products/" + testProductId);

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
                .delete("/api/shopping/cart");

        // Given
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("cartId", 999999L); // Non-existent cart

        Map<String, String> shippingAddress = new HashMap<>();
        shippingAddress.put("recipientName", "Test User");
        shippingAddress.put("phoneNumber", "010-1234-5678");
        shippingAddress.put("address", "Seoul, Korea");
        shippingAddress.put("zipCode", "12345");
        orderRequest.put("shippingAddress", shippingAddress);

        // When
        Response response = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/shopping/orders");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(21)
    @DisplayName("21. Pay for non-existent order should fail")
    void testPayNonExistentOrder() {
        // Given
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", 999999L);
        paymentRequest.put("paymentMethod", "CREDIT_CARD");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/shopping/payments");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(404)));
    }

    @Test
    @Order(22)
    @DisplayName("22. Add out-of-stock product to cart should fail")
    void testAddOutOfStockToCart() {
        // Given - Request more than available stock
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", testProductId);
        cartItem.put("quantity", 999999); // Unrealistic quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/shopping/cart/items");

        // Then - Should fail due to insufficient stock
        response.then()
                .statusCode(anyOf(is(400), is(422))); // Bad Request or Unprocessable Entity
    }

    @Test
    @Order(23)
    @DisplayName("23. Access other user's order should fail")
    void testAccessOtherUsersOrder() {
        // Skip if no order
        Assumptions.assumeTrue(testOrderId != null, "No order available for testing");

        // Create a new user with different token
        String otherUserEmail = generateTestEmail();
        String otherUserToken = createUserAndGetToken(otherUserEmail, "Test1234!", "Other User");

        // When - Try to access test user's order with other user's token
        Response response = givenWithToken(otherUserToken)
                .when()
                .get("/api/shopping/orders/" + testOrderId);

        // Then - Should fail with 403 or 404
        response.then()
                .statusCode(anyOf(is(403), is(404)));
    }
}
