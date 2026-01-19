package com.portal.universe.integration.flows;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Error Case Integration Tests
 *
 * Verifies proper error handling across all services:
 * - Authentication errors (401, 403)
 * - Business logic errors (400, 404, 409)
 * - Proper error code responses
 */
@Slf4j
@DisplayName("Error Case Integration Tests")
@Tag("flow")
@Tag("error")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ErrorCaseTest extends IntegrationTestBase {

    // =======================================
    // Authentication Errors (401)
    // =======================================

    @Test
    @Order(1)
    @DisplayName("401: Invalid credentials should return Unauthorized")
    void testInvalidCredentials() {
        // Given
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", TEST_USER_EMAIL);
        credentials.put("password", "WrongPassword123!");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(credentials)
                .when()
                .post("/api/auth/login");

        // Then
        response.then()
                .statusCode(401);

        log.info("401 test passed - invalid credentials rejected");
    }

    @Test
    @Order(2)
    @DisplayName("401: Expired token should return Unauthorized")
    void testExpiredToken() {
        // Given - Create an expired-like token (malformed)
        String expiredToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiZXhwIjoxNjAwMDAwMDAwfQ." +
                "invalid_signature";

        // When
        Response response = givenWithToken(expiredToken)
                .when()
                .get("/api/auth/me");

        // Then
        response.then()
                .statusCode(401);

        log.info("401 test passed - expired token rejected");
    }

    @Test
    @Order(3)
    @DisplayName("401: Missing token should return Unauthorized")
    void testMissingToken() {
        // When
        Response response = givenUnauthenticated()
                .when()
                .get("/api/auth/me");

        // Then
        response.then()
                .statusCode(401);

        log.info("401 test passed - missing token rejected");
    }

    @Test
    @Order(4)
    @DisplayName("401: Malformed token should return Unauthorized")
    void testMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When
        Response response = givenWithToken(malformedToken)
                .when()
                .get("/api/auth/me");

        // Then
        response.then()
                .statusCode(401);

        log.info("401 test passed - malformed token rejected");
    }

    // =======================================
    // Authorization Errors (403)
    // =======================================

    @Test
    @Order(10)
    @DisplayName("403: Regular user accessing admin endpoint")
    void testUserAccessingAdminEndpoint() {
        // When - Regular user tries to access admin endpoint
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/admin/products");

        // Then
        response.then()
                .statusCode(403);

        log.info("403 test passed - admin endpoint protected");
    }

    @Test
    @Order(11)
    @DisplayName("403: User cannot create coupons")
    void testUserCannotCreateCoupon() {
        // Given
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Unauthorized Coupon");
        couponRequest.put("code", "UNAUTH" + System.currentTimeMillis());
        couponRequest.put("discountType", "FIXED");
        couponRequest.put("discountValue", 1000);
        couponRequest.put("totalQuantity", 100);

        // When
        Response response = givenAuthenticatedUser()
                .body(couponRequest)
                .when()
                .post("/api/shopping/admin/coupons");

        // Then
        response.then()
                .statusCode(403);

        log.info("403 test passed - user cannot create coupons");
    }

    @Test
    @Order(12)
    @DisplayName("403: User cannot access other user's order")
    void testUserCannotAccessOtherUserOrder() {
        // First, create an order for the test user
        Response productsResponse = givenUnauthenticated()
                .when()
                .get("/api/shopping/products");

        if (productsResponse.statusCode() != 200) {
            log.warn("No products available for test");
            return;
        }

        Long productId = productsResponse.jsonPath().getLong("data.content[0].id");

        // Create cart and order for test user
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("quantity", 1);

        Response cartResponse = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/shopping/cart/items");

        Long cartId = cartResponse.jsonPath().getLong("data.cartId");

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("cartId", cartId);
        orderRequest.put("shippingAddress", Map.of(
                "recipientName", "Test",
                "phoneNumber", "010-1234-5678",
                "address", "Seoul",
                "zipCode", "12345"
        ));

        Response orderResponse = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/shopping/orders");

        if (orderResponse.statusCode() != 200 && orderResponse.statusCode() != 201) {
            log.warn("Order creation failed for test");
            return;
        }

        Long orderId = orderResponse.jsonPath().getLong("data.id");

        // Create another user
        String otherEmail = generateTestEmail();
        String otherToken = createUserAndGetToken(otherEmail, "Test1234!", "Other User");

        // When - Other user tries to access the order
        Response response = givenWithToken(otherToken)
                .when()
                .get("/api/shopping/orders/" + orderId);

        // Then - Should be 403 or 404
        response.then()
                .statusCode(anyOf(is(403), is(404)));

        log.info("403/404 test passed - cross-user order access blocked");
    }

    // =======================================
    // Not Found Errors (404)
    // =======================================

    @Test
    @Order(20)
    @DisplayName("404: Non-existent product - S001")
    void testProductNotFound() {
        // When
        Response response = givenUnauthenticated()
                .when()
                .get("/api/shopping/products/999999999");

        // Then
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("code", equalTo("S001")); // PRODUCT_NOT_FOUND

        log.info("404 test passed - S001 for non-existent product");
    }

    @Test
    @Order(21)
    @DisplayName("404: Non-existent order")
    void testOrderNotFound() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/999999999");

        // Then
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("code", equalTo("S201")); // ORDER_NOT_FOUND

        log.info("404 test passed - S201 for non-existent order");
    }

    @Test
    @Order(22)
    @DisplayName("404: Non-existent coupon - S601")
    void testCouponNotFound() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/999999999/issue");

        // Then
        response.then()
                .statusCode(anyOf(is(404), is(400)))
                .body("success", is(false))
                .body("code", equalTo("S601")); // COUPON_NOT_FOUND

        log.info("404 test passed - S601 for non-existent coupon");
    }

    @Test
    @Order(23)
    @DisplayName("404: Non-existent time-deal - S701")
    void testTimeDealNotFound() {
        // Given
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", 999999999L);
        purchaseRequest.put("quantity", 1);

        // When
        Response response = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Then
        response.then()
                .statusCode(anyOf(is(404), is(400)))
                .body("success", is(false))
                .body("code", equalTo("S701")); // TIMEDEAL_NOT_FOUND

        log.info("404 test passed - S701 for non-existent time-deal");
    }

    // =======================================
    // Business Logic Errors (400)
    // =======================================

    @Test
    @Order(30)
    @DisplayName("400: Invalid email format on signup")
    void testInvalidEmailFormat() {
        // Given
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", "invalid-email");
        signupData.put("password", "Test1234!");
        signupData.put("name", "Test User");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/users/signup");

        // Then
        response.then()
                .statusCode(400);

        log.info("400 test passed - invalid email format rejected");
    }

    @Test
    @Order(31)
    @DisplayName("400: Weak password on signup")
    void testWeakPassword() {
        // Given
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", generateTestEmail());
        signupData.put("password", "weak"); // Too weak
        signupData.put("name", "Test User");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/users/signup");

        // Then
        response.then()
                .statusCode(400);

        log.info("400 test passed - weak password rejected");
    }

    @Test
    @Order(32)
    @DisplayName("400: Invalid quantity (negative) for cart")
    void testInvalidCartQuantity() {
        // Given
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", 1L);
        cartItem.put("quantity", -1); // Negative quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/shopping/cart/items");

        // Then
        response.then()
                .statusCode(400);

        log.info("400 test passed - negative quantity rejected");
    }

    @Test
    @Order(33)
    @DisplayName("400: Zero quantity for cart")
    void testZeroCartQuantity() {
        // Given
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", 1L);
        cartItem.put("quantity", 0); // Zero quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/shopping/cart/items");

        // Then
        response.then()
                .statusCode(400);

        log.info("400 test passed - zero quantity rejected");
    }

    // =======================================
    // Conflict Errors (409)
    // =======================================

    @Test
    @Order(40)
    @DisplayName("409: Duplicate email signup")
    void testDuplicateEmailSignup() {
        // First signup
        String testEmail = generateTestEmail();
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", testEmail);
        signupData.put("password", "Test1234!");
        signupData.put("name", "First User");

        given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/users/signup")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Second signup with same email
        signupData.put("name", "Second User");

        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/users/signup");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(409)));

        log.info("409 test passed - duplicate email rejected");
    }

    // =======================================
    // Inventory Errors
    // =======================================

    @Test
    @Order(50)
    @DisplayName("400: Insufficient stock - S401")
    void testInsufficientStock() {
        // Get a product
        Response productsResponse = givenUnauthenticated()
                .when()
                .get("/api/shopping/products");

        Long productId = productsResponse.jsonPath().getLong("data.content[0].id");

        // Given - Request more than available
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("quantity", 999999); // Unrealistic quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/shopping/cart/items");

        // Then - Should fail due to insufficient stock
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        String code = response.jsonPath().getString("code");
        assertThat(code).isIn("S401", "S402"); // INVENTORY_NOT_FOUND or INSUFFICIENT_STOCK

        log.info("400 test passed - insufficient stock rejected with code: {}", code);
    }

    // =======================================
    // Payment Errors
    // =======================================

    @Test
    @Order(60)
    @DisplayName("400: Payment for non-existent order - S201")
    void testPaymentForNonExistentOrder() {
        // Given
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", 999999999L);
        paymentRequest.put("paymentMethod", "CREDIT_CARD");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/shopping/payments");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(404)))
                .body("success", is(false));

        log.info("400/404 test passed - payment for non-existent order rejected");
    }

    @Test
    @Order(61)
    @DisplayName("400: Invalid payment method")
    void testInvalidPaymentMethod() {
        // Get a valid order first (if exists)
        Response ordersResponse = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/orders/my");

        if (ordersResponse.jsonPath().getList("data.content") == null ||
            ordersResponse.jsonPath().getList("data.content").isEmpty()) {
            log.warn("No orders available for payment test");
            return;
        }

        Long orderId = ordersResponse.jsonPath().getLong("data.content[0].id");

        // Given - Invalid payment method
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", orderId);
        paymentRequest.put("paymentMethod", "INVALID_METHOD");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/shopping/payments");

        // Then
        response.then()
                .statusCode(400);

        log.info("400 test passed - invalid payment method rejected");
    }

    // =======================================
    // Rate Limiting (if implemented)
    // =======================================

    @Test
    @Order(70)
    @DisplayName("429: Rate limiting (if implemented)")
    void testRateLimiting() {
        // Make many rapid requests
        int requestCount = 100;
        int tooManyRequestsCount = 0;

        for (int i = 0; i < requestCount; i++) {
            Response response = givenAuthenticatedUser()
                    .when()
                    .get("/api/shopping/products");

            if (response.statusCode() == 429) {
                tooManyRequestsCount++;
            }
        }

        if (tooManyRequestsCount > 0) {
            log.info("429 test passed - rate limiting active, {} requests limited", tooManyRequestsCount);
        } else {
            log.info("Rate limiting not implemented or threshold not reached");
        }
    }

    // =======================================
    // Error Response Format Validation
    // =======================================

    @Test
    @Order(80)
    @DisplayName("Error response should follow ApiResponse format")
    void testErrorResponseFormat() {
        // When - Request non-existent resource
        Response response = givenUnauthenticated()
                .when()
                .get("/api/shopping/products/999999999");

        // Then - Verify ApiResponse format
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("code", notNullValue())
                .body("message", notNullValue());

        String code = response.jsonPath().getString("code");
        String message = response.jsonPath().getString("message");

        assertThat(code).matches("[A-Z]\\d{3}"); // Format: Letter + 3 digits
        assertThat(message).isNotBlank();

        log.info("Error response format validated - code: {}, message: {}", code, message);
    }
}
