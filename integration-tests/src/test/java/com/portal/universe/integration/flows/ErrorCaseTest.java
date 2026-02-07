package com.portal.universe.integration.flows;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
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
                .post("/api/v1/auth/login");

        // Then - 401 for invalid credentials, or 429 if rate-limited
        response.then()
                .statusCode(anyOf(is(401), is(429)));

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

        // When - Access via gateway with protected endpoint
        // Note: /api/v1/users/** is permit-all in gateway, so use shopping endpoint
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + expiredToken)
                .when()
                .get("/api/v1/shopping/orders");

        // Then
        response.then()
                .statusCode(401);

        log.info("401 test passed - expired token rejected");
    }

    @Test
    @Order(3)
    @DisplayName("401: Missing token should return Unauthorized")
    void testMissingToken() {
        // When - Access via gateway with protected endpoint
        // Note: /api/v1/users/** is permit-all in gateway, so use shopping endpoint
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType("application/json")
                .when()
                .get("/api/v1/shopping/orders");

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

        // When - Access via gateway with protected endpoint
        // Note: /api/v1/users/** is permit-all in gateway, so use shopping endpoint
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType("application/json")
                .header("Authorization", "Bearer " + malformedToken)
                .when()
                .get("/api/v1/shopping/orders");

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
                .get("/api/v1/shopping/admin/products");

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
        couponRequest.put("startsAt", java.time.LocalDateTime.now().toString());
        couponRequest.put("expiresAt", java.time.LocalDateTime.now().plusDays(7).toString());

        // When
        Response response = givenAuthenticatedUser()
                .body(couponRequest)
                .when()
                .post("/api/v1/shopping/admin/coupons");

        // Then
        response.then()
                .statusCode(403);

        log.info("403 test passed - user cannot create coupons");
    }

    @Test
    @Order(12)
    @DisplayName("403: User cannot access other user's order")
    void testUserCannotAccessOtherUserOrder() {
        // First, check if products are available
        Response productsResponse = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products");

        if (productsResponse.statusCode() != 200) {
            log.warn("No products available for test");
            return;
        }

        List<Map<String, Object>> products = productsResponse.jsonPath().getList("data.content");
        if (products == null || products.isEmpty()) {
            log.warn("Products list is empty, skipping test");
            return;
        }

        Long productId = Long.valueOf(products.get(0).get("id").toString());

        // Add to cart for test user
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("quantity", 1);

        givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/v1/shopping/cart/items");

        // Checkout cart
        givenAuthenticatedUser()
                .when()
                .post("/api/v1/shopping/cart/checkout");

        // Create order (no cartId needed, cart is already checked out)
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("shippingAddress", Map.of(
                "receiverName", "Test",
                "receiverPhone", "010-1234-5678",
                "address1", "Seoul",
                "zipCode", "12345"
        ));

        Response orderResponse = givenAuthenticatedUser()
                .body(orderRequest)
                .when()
                .post("/api/v1/shopping/orders");

        if (orderResponse.statusCode() != 200 && orderResponse.statusCode() != 201) {
            log.warn("Order creation failed for test: {}", orderResponse.body().asString());
            return;
        }

        String orderNumber = orderResponse.jsonPath().getString("data.orderNumber");

        // Create another user
        String otherEmail = generateTestEmail();
        String otherToken = createUserAndGetToken(otherEmail, "SecurePw8!", "Other User");

        // When - Other user tries to access the order
        Response response = givenWithToken(otherToken)
                .when()
                .get("/api/v1/shopping/orders/" + orderNumber);

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
                .get("/api/v1/shopping/products/999999999");

        // Then
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("error.code", equalTo("S001")); // PRODUCT_NOT_FOUND

        log.info("404 test passed - S001 for non-existent product");
    }

    @Test
    @Order(21)
    @DisplayName("404: Non-existent order")
    void testOrderNotFound() {
        // When - Use a non-existent order number
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders/NONEXISTENT999");

        // Then
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("error.code", equalTo("S201")); // ORDER_NOT_FOUND

        log.info("404 test passed - S201 for non-existent order");
    }

    @Test
    @Order(22)
    @DisplayName("404: Non-existent coupon - S601")
    void testCouponNotFound() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/v1/shopping/coupons/999999999/issue");

        // Then
        response.then()
                .statusCode(anyOf(is(404), is(400)))
                .body("success", is(false))
                .body("error.code", equalTo("S601")); // COUPON_NOT_FOUND

        log.info("404 test passed - S601 for non-existent coupon");
    }

    @Test
    @Order(23)
    @DisplayName("404: Non-existent time-deal - S701")
    void testTimeDealNotFound() {
        // Given - Use a non-existent timeDealProductId
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealProductId", 999999999L);
        purchaseRequest.put("quantity", 1);

        // When
        Response response = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/v1/shopping/time-deals/purchase");

        // Then
        response.then()
                .statusCode(anyOf(is(404), is(400)))
                .body("success", is(false))
                .body("error.code", equalTo("S706")); // TIMEDEAL_PRODUCT_NOT_FOUND

        log.info("404 test passed - S706 for non-existent time-deal product");
    }

    // =======================================
    // Business Logic Errors (400)
    // =======================================

    @Test
    @Order(30)
    @DisplayName("400: Invalid email format on signup")
    void testInvalidEmailFormat() {
        // Given
        Map<String, Object> signupData = new HashMap<>();
        signupData.put("email", "invalid-email");
        signupData.put("password", "SecurePw8!");
        signupData.put("nickname", "Test User");
        signupData.put("realName", "Test User");
        signupData.put("marketingAgree", false);

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

        // Then - 400 for validation error, or 409 if email already exists from previous test run
        response.then()
                .statusCode(anyOf(is(400), is(409)));

        log.info("400 test passed - invalid email format rejected");
    }

    @Test
    @Order(31)
    @DisplayName("400: Weak password on signup")
    void testWeakPassword() {
        // Given
        Map<String, Object> signupData = new HashMap<>();
        signupData.put("email", generateTestEmail());
        signupData.put("password", "weak"); // Too weak
        signupData.put("nickname", "Test User");
        signupData.put("realName", "Test User");
        signupData.put("marketingAgree", false);

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

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
                .post("/api/v1/shopping/cart/items");

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
                .post("/api/v1/shopping/cart/items");

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
        Map<String, Object> signupData = new HashMap<>();
        signupData.put("email", testEmail);
        signupData.put("password", "SecurePw8!");
        signupData.put("nickname", "First User");
        signupData.put("realName", "First User");
        signupData.put("marketingAgree", false);

        given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/v1/users/signup")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Second signup with same email
        signupData.put("nickname", "Second User");
        signupData.put("realName", "Second User");

        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType("application/json")
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

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
                .get("/api/v1/shopping/products");

        List<Map<String, Object>> products = productsResponse.jsonPath().getList("data.content");
        if (products == null || products.isEmpty()) {
            log.warn("No products available for insufficient stock test, skipping");
            return;
        }

        Long productId = Long.valueOf(products.get(0).get("id").toString());

        // Given - Request more than available
        Map<String, Object> cartItem = new HashMap<>();
        cartItem.put("productId", productId);
        cartItem.put("quantity", 999999); // Unrealistic quantity

        // When
        Response response = givenAuthenticatedUser()
                .body(cartItem)
                .when()
                .post("/api/v1/shopping/cart/items");

        // Then - Should fail due to insufficient stock
        response.then()
                .statusCode(anyOf(is(400), is(422)));

        String code = response.jsonPath().getString("error.code");
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
        // Given - Use a non-existent order number
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderNumber", "NONEXISTENT-ORDER-999");
        paymentRequest.put("paymentMethod", "CARD");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/v1/shopping/payments");

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
        // Get user orders
        Response ordersResponse = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/orders");

        List<Map<String, Object>> orders = ordersResponse.jsonPath().getList("data.content");
        if (orders == null || orders.isEmpty()) {
            log.warn("No orders available for payment test");
            return;
        }

        String orderNumber = orders.get(0).get("orderNumber").toString();

        // Given - Invalid payment method
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderNumber", orderNumber);
        paymentRequest.put("paymentMethod", "INVALID_METHOD");

        // When
        Response response = givenAuthenticatedUser()
                .body(paymentRequest)
                .when()
                .post("/api/v1/shopping/payments");

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
                    .get("/api/v1/shopping/products");

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
                .get("/api/v1/shopping/products/999999999");

        // Then - Verify ApiResponse format
        response.then()
                .statusCode(404)
                .body("success", is(false))
                .body("error.code", notNullValue())
                .body("error.message", notNullValue());

        String code = response.jsonPath().getString("error.code");
        String message = response.jsonPath().getString("error.message");

        assertThat(code).matches("[A-Z]\\d{3}"); // Format: Letter + 3 digits
        assertThat(message).isNotBlank();

        log.info("Error response format validated - code: {}, message: {}", code, message);
    }
}
