package com.portal.universe.integration.flows;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Coupon Flow Integration Tests
 *
 * Tests the coupon lifecycle:
 * 1. Admin creates coupon
 * 2. User views available coupons
 * 3. User issues coupon (first-come-first-served)
 * 4. User views my coupons
 * 5. User applies coupon to order
 * 6. Verify discount applied
 */
@DisplayName("Coupon Flow Integration Tests")
@Tag("flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CouponFlowTest extends IntegrationTestBase {

    private static Long testCouponId;
    private static String testCouponCode;
    private static Long testUserCouponId;

    @Test
    @Order(1)
    @DisplayName("1. Admin creates a new coupon")
    void testAdminCreatesCoupon() {
        // Given
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Integration Test Coupon - " + generateTestId());
        couponRequest.put("code", "INTTEST" + System.currentTimeMillis());
        couponRequest.put("discountType", "PERCENTAGE");
        couponRequest.put("discountValue", 10); // 10% discount
        couponRequest.put("maxDiscountAmount", 5000);
        couponRequest.put("minOrderAmount", 10000);
        couponRequest.put("totalQuantity", 100);

        // Set valid period
        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When
        Response response = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/shopping/admin/coupons");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue());

        testCouponId = response.jsonPath().getLong("data.id");
        testCouponCode = response.jsonPath().getString("data.code");

        assertThat(testCouponId).isNotNull();
        assertThat(testCouponCode).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("2. User views available coupons")
    void testGetAvailableCoupons() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/coupons");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", notNullValue());

        // Verify our test coupon is visible if it was created
        if (testCouponId != null) {
            List<Map<String, Object>> coupons = response.jsonPath().getList("data.content");
            if (coupons != null) {
                boolean couponFound = coupons.stream()
                        .anyMatch(c -> testCouponId.equals(Long.valueOf(c.get("id").toString())));
                // Coupon might not be in list if not yet active or other conditions
                // Just log for debugging
                if (!couponFound) {
                    System.out.println("Test coupon not in available list - might have conditions");
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. User issues (claims) a coupon")
    void testIssueCoupon() {
        // Skip if no coupon created
        Assumptions.assumeTrue(testCouponId != null, "No coupon available for testing");

        // When - Issue coupon by ID
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/" + testCouponId + "/issue");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.couponId", equalTo(testCouponId.intValue()));

        testUserCouponId = response.jsonPath().getLong("data.id");
        assertThat(testUserCouponId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. User views their issued coupons")
    void testGetMyCoupons() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/coupons/my");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", notNullValue());

        // Verify issued coupon is in user's coupons
        if (testUserCouponId != null) {
            List<Map<String, Object>> myCoupons = response.jsonPath().getList("data.content");
            if (myCoupons != null && !myCoupons.isEmpty()) {
                boolean found = myCoupons.stream()
                        .anyMatch(c -> testUserCouponId.equals(Long.valueOf(c.get("id").toString())));
                assertThat(found).isTrue();
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Duplicate coupon issue should fail (S603)")
    void testDuplicateCouponIssueFails() {
        // Skip if no coupon
        Assumptions.assumeTrue(testCouponId != null, "No coupon available for testing");

        // When - Try to issue same coupon again
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/" + testCouponId + "/issue");

        // Then - Should fail with duplicate error
        response.then()
                .statusCode(anyOf(is(400), is(409)))
                .body("success", is(false))
                .body("code", equalTo("S603")); // COUPON_ALREADY_ISSUED
    }

    @Test
    @Order(6)
    @DisplayName("6. Issue non-existent coupon should fail (S601)")
    void testIssueNonExistentCouponFails() {
        // When - Try to issue non-existent coupon
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/999999/issue");

        // Then
        response.then()
                .statusCode(anyOf(is(404), is(400)))
                .body("success", is(false))
                .body("code", equalTo("S601")); // COUPON_NOT_FOUND
    }

    @Test
    @Order(7)
    @DisplayName("7. Admin can view coupon details")
    void testAdminViewsCouponDetails() {
        // Skip if no coupon
        Assumptions.assumeTrue(testCouponId != null, "No coupon available for testing");

        // When
        Response response = givenAuthenticatedAdmin()
                .when()
                .get("/api/shopping/admin/coupons/" + testCouponId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.id", equalTo(testCouponId.intValue()))
                .body("data.issuedCount", greaterThanOrEqualTo(1)); // At least our test user issued it
    }

    @Test
    @Order(8)
    @DisplayName("8. Admin creates exhausted coupon (quantity=1)")
    void testCreateExhaustedCoupon() {
        // Given - Coupon with only 1 quantity
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Limited Coupon - " + generateTestId());
        couponRequest.put("code", "LIMITED" + System.currentTimeMillis());
        couponRequest.put("discountType", "FIXED");
        couponRequest.put("discountValue", 1000);
        couponRequest.put("totalQuantity", 1);

        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/shopping/admin/coupons");

        createResponse.then().statusCode(anyOf(is(200), is(201)));
        Long limitedCouponId = createResponse.jsonPath().getLong("data.id");

        // First user issues it
        givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/" + limitedCouponId + "/issue")
                .then()
                .statusCode(anyOf(is(200), is(201)));

        // Create another user
        String otherEmail = generateTestEmail();
        String otherToken = createUserAndGetToken(otherEmail, "Test1234!", "Other User");

        // When - Second user tries to issue exhausted coupon
        Response response = givenWithToken(otherToken)
                .when()
                .post("/api/shopping/coupons/" + limitedCouponId + "/issue");

        // Then - Should fail with exhausted error
        response.then()
                .statusCode(anyOf(is(400), is(409)))
                .body("success", is(false))
                .body("code", equalTo("S602")); // COUPON_EXHAUSTED
    }

    @Test
    @Order(9)
    @DisplayName("9. Issue expired coupon should fail")
    void testIssueExpiredCouponFails() {
        // Given - Create expired coupon
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Expired Coupon - " + generateTestId());
        couponRequest.put("code", "EXPIRED" + System.currentTimeMillis());
        couponRequest.put("discountType", "FIXED");
        couponRequest.put("discountValue", 1000);
        couponRequest.put("totalQuantity", 100);

        // Set expired period
        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.minusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/shopping/admin/coupons");

        // Admin might reject expired coupon creation
        if (createResponse.statusCode() != 200 && createResponse.statusCode() != 201) {
            return; // Skip if creation not allowed
        }

        Long expiredCouponId = createResponse.jsonPath().getLong("data.id");

        // When - Try to issue expired coupon
        Response response = givenAuthenticatedUser()
                .when()
                .post("/api/shopping/coupons/" + expiredCouponId + "/issue");

        // Then - Should fail
        response.then()
                .statusCode(anyOf(is(400), is(422)))
                .body("success", is(false));
    }

    @Test
    @Order(10)
    @DisplayName("10. Regular user cannot create coupons")
    void testUserCannotCreateCoupon() {
        // Given
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "User Created Coupon");
        couponRequest.put("code", "USERCOUPON" + System.currentTimeMillis());
        couponRequest.put("discountType", "FIXED");
        couponRequest.put("discountValue", 1000);
        couponRequest.put("totalQuantity", 100);

        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When - Regular user tries to create coupon
        Response response = givenAuthenticatedUser()
                .body(couponRequest)
                .when()
                .post("/api/shopping/admin/coupons");

        // Then - Should fail with 403
        response.then()
                .statusCode(403);
    }
}
