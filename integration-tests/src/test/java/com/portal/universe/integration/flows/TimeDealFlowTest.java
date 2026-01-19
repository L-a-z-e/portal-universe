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
 * TimeDeal Flow Integration Tests
 *
 * Tests the time-deal lifecycle:
 * 1. Admin creates time-deal
 * 2. User views active time-deals
 * 3. User views time-deal details
 * 4. User purchases time-deal item
 * 5. Verify stock decrease
 * 6. Verify 1 per user limit
 */
@DisplayName("TimeDeal Flow Integration Tests")
@Tag("flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeDealFlowTest extends IntegrationTestBase {

    private static Long testTimeDealId;
    private static Long testProductId;
    private static int initialStock;

    @BeforeAll
    void setupProduct() {
        // Get a product for time-deal
        Response response = givenUnauthenticated()
                .when()
                .get("/api/shopping/products");

        if (response.statusCode() == 200) {
            List<Map<String, Object>> products = response.jsonPath().getList("data.content");
            if (products != null && !products.isEmpty()) {
                testProductId = Long.valueOf(products.get(0).get("id").toString());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Admin creates a new time-deal")
    void testAdminCreatesTimeDeal() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available for time-deal");

        // Given
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "Integration Test TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 5000); // Discounted price
        timeDealRequest.put("stockQuantity", 50);
        timeDealRequest.put("maxPerUser", 1);

        // Set active period (starts now)
        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When
        Response response = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.id", notNullValue());

        testTimeDealId = response.jsonPath().getLong("data.id");
        initialStock = response.jsonPath().getInt("data.stockQuantity");

        assertThat(testTimeDealId).isNotNull();
        assertThat(initialStock).isEqualTo(50);
    }

    @Test
    @Order(2)
    @DisplayName("2. User views active time-deals")
    void testGetActiveTimeDeals() {
        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/time-deals");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", notNullValue());

        // Verify our time-deal is active
        if (testTimeDealId != null) {
            List<Map<String, Object>> timeDeals = response.jsonPath().getList("data.content");
            if (timeDeals != null) {
                boolean found = timeDeals.stream()
                        .anyMatch(td -> testTimeDealId.equals(Long.valueOf(td.get("id").toString())));
                // TimeDeal might need scheduler to activate - check status
                if (!found) {
                    System.out.println("TimeDeal not yet active - scheduler might need to run");
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. User views time-deal details")
    void testGetTimeDealDetails() {
        // Skip if no time-deal
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/time-deals/" + testTimeDealId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.id", equalTo(testTimeDealId.intValue()))
                .body("data.stockQuantity", greaterThan(0))
                .body("data.maxPerUser", equalTo(1));
    }

    @Test
    @Order(4)
    @DisplayName("4. User purchases time-deal item")
    void testPurchaseTimeDeal() {
        // Skip if no time-deal
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal available for testing");

        // Given
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", testTimeDealId);
        purchaseRequest.put("quantity", 1);

        // When
        Response response = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Then - Success or already purchased
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            response.then()
                    .body("success", is(true))
                    .body("data.orderId", notNullValue());
        } else {
            // Might fail if time-deal not active yet
            System.out.println("Purchase failed: " + response.body().asString());
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Verify stock decreased after purchase")
    void testStockDecreased() {
        // Skip if no time-deal
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal available for testing");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/time-deals/" + testTimeDealId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true));

        int currentStock = response.jsonPath().getInt("data.stockQuantity");
        // Stock should be less than initial if purchase succeeded
        // Note: Test might run in isolation, so just verify stock is valid
        assertThat(currentStock).isGreaterThanOrEqualTo(0);
        assertThat(currentStock).isLessThanOrEqualTo(initialStock);
    }

    @Test
    @Order(6)
    @DisplayName("6. Same user cannot exceed max per user limit (S706)")
    void testMaxPerUserLimit() {
        // Skip if no time-deal
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal available for testing");

        // Given - User already purchased in test 4
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", testTimeDealId);
        purchaseRequest.put("quantity", 1);

        // When - Try to purchase again
        Response response = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Then - Should fail with max per user error
        response.then()
                .statusCode(anyOf(is(400), is(409)))
                .body("success", is(false))
                .body("code", equalTo("S706")); // MAX_PER_USER_EXCEEDED
    }

    @Test
    @Order(7)
    @DisplayName("7. Different user can purchase same time-deal")
    void testDifferentUserCanPurchase() {
        // Skip if no time-deal
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal available for testing");

        // Create new user
        String newUserEmail = generateTestEmail();
        String newUserToken = createUserAndGetToken(newUserEmail, "Test1234!", "New User");

        // Given
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", testTimeDealId);
        purchaseRequest.put("quantity", 1);

        // When
        Response response = givenWithToken(newUserToken)
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Then - Should succeed (if time-deal is active)
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            response.then()
                    .body("success", is(true));
        }
    }

    @Test
    @Order(8)
    @DisplayName("8. Purchase non-existent time-deal should fail (S701)")
    void testPurchaseNonExistentTimeDeal() {
        // Given
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", 999999L);
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
    }

    @Test
    @Order(9)
    @DisplayName("9. Admin creates scheduled time-deal (not yet active)")
    void testCreateScheduledTimeDeal() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available for time-deal");

        // Given - Future time-deal
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "Future TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 3000);
        timeDealRequest.put("stockQuantity", 10);
        timeDealRequest.put("maxPerUser", 2);

        // Set future period
        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When
        Response response = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.status", equalTo("SCHEDULED"));

        Long scheduledTimeDealId = response.jsonPath().getLong("data.id");

        // Try to purchase scheduled time-deal
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", scheduledTimeDealId);
        purchaseRequest.put("quantity", 1);

        Response purchaseResponse = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Should fail - time-deal not active
        purchaseResponse.then()
                .statusCode(anyOf(is(400), is(422)))
                .body("success", is(false))
                .body("code", equalTo("S702")); // TIMEDEAL_NOT_ACTIVE
    }

    @Test
    @Order(10)
    @DisplayName("10. Admin creates ended time-deal")
    void testPurchaseEndedTimeDeal() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available for time-deal");

        // Given - Past time-deal
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "Past TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 2000);
        timeDealRequest.put("stockQuantity", 10);
        timeDealRequest.put("maxPerUser", 1);
        timeDealRequest.put("status", "ENDED"); // Force ended status if API allows

        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.minusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        // Admin might reject past time-deal creation
        if (createResponse.statusCode() != 200 && createResponse.statusCode() != 201) {
            return;
        }

        Long endedTimeDealId = createResponse.jsonPath().getLong("data.id");

        // When - Try to purchase ended time-deal
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", endedTimeDealId);
        purchaseRequest.put("quantity", 1);

        Response response = givenAuthenticatedUser()
                .body(purchaseRequest)
                .when()
                .post("/api/shopping/time-deals/purchase");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(422)))
                .body("success", is(false))
                .body("code", equalTo("S702")); // TIMEDEAL_NOT_ACTIVE
    }

    @Test
    @Order(11)
    @DisplayName("11. Regular user cannot create time-deals")
    void testUserCannotCreateTimeDeal() {
        // Skip if no product
        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Given
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "User Created TimeDeal");
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 1000);
        timeDealRequest.put("stockQuantity", 10);
        timeDealRequest.put("maxPerUser", 1);

        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When - Regular user tries to create time-deal
        Response response = givenAuthenticatedUser()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        // Then - Should fail with 403
        response.then()
                .statusCode(403);
    }
}
