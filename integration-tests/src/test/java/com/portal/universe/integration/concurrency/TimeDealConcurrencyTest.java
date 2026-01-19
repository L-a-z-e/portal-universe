package com.portal.universe.integration.concurrency;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * TimeDeal Concurrency Tests
 *
 * Tests for time-deal purchase under high concurrency:
 * - Exactly N items sold when N available (no overselling)
 * - 1 per user limit enforcement under concurrency
 * - Redis Lua script atomicity verification
 */
@Slf4j
@DisplayName("TimeDeal Concurrency Tests")
@Tag("concurrency")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeDealConcurrencyTest extends IntegrationTestBase {

    private static final int TIMEDEAL_STOCK = 100;
    private static final int CONCURRENT_USERS = 150;
    private static final int MAX_PER_USER = 1;

    private static Long testTimeDealId;
    private static Long testProductId;
    private static List<String> testUserTokens = new ArrayList<>();

    @BeforeAll
    void setupTestData() {
        // Get a product for time-deal
        Response productResponse = givenUnauthenticated()
                .when()
                .get("/api/shopping/products");

        if (productResponse.statusCode() == 200) {
            List<Map<String, Object>> products = productResponse.jsonPath().getList("data.content");
            if (products != null && !products.isEmpty()) {
                testProductId = Long.valueOf(products.get(0).get("id").toString());
            }
        }

        Assumptions.assumeTrue(testProductId != null, "No product available for testing");

        // Create test users
        log.info("Creating {} test users for TimeDeal concurrency test...", CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String email = "timedeal-test-" + index + "-" + System.currentTimeMillis() + "@test.com";
                try {
                    return createUserAndGetToken(email, "Test1234!", "TimeDeal User " + index);
                } catch (Exception e) {
                    log.warn("Failed to create user {}: {}", index, e.getMessage());
                    return null;
                }
            }));
        }

        for (Future<String> future : futures) {
            try {
                String token = future.get(30, TimeUnit.SECONDS);
                if (token != null) {
                    testUserTokens.add(token);
                }
            } catch (Exception e) {
                log.warn("Failed to get user token: {}", e.getMessage());
            }
        }

        executor.shutdown();
        log.info("Created {} test users", testUserTokens.size());
    }

    @Test
    @Order(1)
    @DisplayName("1. Setup: Admin creates time-deal with limited stock")
    void setupTimeDeal() {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Given
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "Concurrency Test TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 5000);
        timeDealRequest.put("stockQuantity", TIMEDEAL_STOCK);
        timeDealRequest.put("maxPerUser", MAX_PER_USER);

        // Active period (starts now)
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
                .body("success", is(true));

        testTimeDealId = response.jsonPath().getLong("data.id");
        assertThat(testTimeDealId).isNotNull();

        log.info("Created time-deal with ID: {}, stock: {}, maxPerUser: {}",
                testTimeDealId, TIMEDEAL_STOCK, MAX_PER_USER);
    }

    @Test
    @Order(2)
    @DisplayName("2. 150 concurrent users purchase 100 items - exactly 100 should succeed")
    void testConcurrentTimeDealPurchase() throws InterruptedException {
        // Skip if setup failed
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal created");
        Assumptions.assumeTrue(testUserTokens.size() >= CONCURRENT_USERS,
                "Not enough test users: " + testUserTokens.size());

        log.info("Starting concurrent time-deal purchase test: {} users, {} stock",
                CONCURRENT_USERS, TIMEDEAL_STOCK);

        // Given
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);

        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", testTimeDealId);
        purchaseRequest.put("quantity", 1);

        // When - All users try to purchase simultaneously
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final String token = testUserTokens.get(i);
            final int userId = i;

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    Response response = givenWithToken(token)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/shopping/time-deals/purchase");

                    int status = response.statusCode();
                    String code = response.jsonPath().getString("code");

                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                        log.debug("User {} successfully purchased", userId);
                    } else if ("S704".equals(code) || "S705".equals(code)) {
                        // S704: OUT_OF_STOCK, S705: INSUFFICIENT_STOCK
                        outOfStockCount.incrementAndGet();
                        log.debug("User {} - out of stock", userId);
                    } else {
                        errorCount.incrementAndGet();
                        log.debug("User {} - other failure: {} {}", userId, status, code);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("User {} error: {}", userId, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all threads to be ready
        readyLatch.await(30, TimeUnit.SECONDS);

        // Start all threads simultaneously
        log.info("All {} users ready, starting simultaneous purchases...", CONCURRENT_USERS);
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion
        boolean completed = doneLatch.await(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();

        // Then
        log.info("Concurrency test completed in {}ms", duration);
        log.info("Results - Success: {}, Out of Stock: {}, Error: {}",
                successCount.get(), outOfStockCount.get(), errorCount.get());

        assertThat(completed).isTrue().withFailMessage("Test timed out");

        // CRITICAL: Exactly TIMEDEAL_STOCK should succeed (no overselling)
        assertThat(successCount.get())
                .isEqualTo(TIMEDEAL_STOCK)
                .withFailMessage("Expected exactly %d successful purchases, but got %d",
                        TIMEDEAL_STOCK, successCount.get());

        // Remaining users should fail due to out of stock
        assertThat(outOfStockCount.get())
                .isEqualTo(CONCURRENT_USERS - TIMEDEAL_STOCK)
                .withFailMessage("Expected %d out-of-stock failures, but got %d",
                        CONCURRENT_USERS - TIMEDEAL_STOCK, outOfStockCount.get());
    }

    @Test
    @Order(3)
    @DisplayName("3. Verify time-deal stock is exactly zero after concurrent purchase")
    void testTimeDealStockZero() {
        Assumptions.assumeTrue(testTimeDealId != null, "No time-deal created");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/shopping/time-deals/" + testTimeDealId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true));

        int remainingStock = response.jsonPath().getInt("data.stockQuantity");
        log.info("TimeDeal remaining stock: {}", remainingStock);

        assertThat(remainingStock).isZero();
    }

    @Test
    @Order(4)
    @DisplayName("4. Same user cannot purchase twice (maxPerUser enforcement)")
    void testMaxPerUserEnforcement() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Create new time-deal for this test
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "MaxPerUser Test TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 3000);
        timeDealRequest.put("stockQuantity", 20);
        timeDealRequest.put("maxPerUser", 1);

        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        Long maxPerUserTimeDealId = createResponse.jsonPath().getLong("data.id");

        // Single user tries to purchase 5 times concurrently
        String singleUserToken = testUserTokens.get(0);
        final int CONCURRENT_ATTEMPTS = 5;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger limitExceededCount = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(CONCURRENT_ATTEMPTS);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);

        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", maxPerUserTimeDealId);
        purchaseRequest.put("quantity", 1);

        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            executor.submit(() -> {
                try {
                    Response response = givenWithToken(singleUserToken)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/shopping/time-deals/purchase");

                    int status = response.statusCode();
                    String code = response.jsonPath().getString("code");

                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                    } else if ("S706".equals(code)) {
                        // MAX_PER_USER_EXCEEDED
                        limitExceededCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - Only 1 should succeed
        log.info("MaxPerUser test - Success: {}, Limit Exceeded: {}",
                successCount.get(), limitExceededCount.get());

        assertThat(successCount.get())
                .isEqualTo(1)
                .withFailMessage("Expected exactly 1 successful purchase, but got %d", successCount.get());

        assertThat(limitExceededCount.get())
                .isEqualTo(CONCURRENT_ATTEMPTS - 1)
                .withFailMessage("Expected %d limit exceeded failures, but got %d",
                        CONCURRENT_ATTEMPTS - 1, limitExceededCount.get());
    }

    @Test
    @Order(5)
    @DisplayName("5. Test maxPerUser=2 allows 2 purchases per user")
    void testMaxPerUserTwo() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        // Create time-deal with maxPerUser=2
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "MaxPerUser2 Test - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 4000);
        timeDealRequest.put("stockQuantity", 50);
        timeDealRequest.put("maxPerUser", 2); // Allow 2 per user

        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        Long timeDealId = createResponse.jsonPath().getLong("data.id");

        // Single user tries to purchase 5 times
        String singleUserToken = testUserTokens.get(1);
        final int ATTEMPTS = 5;

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(ATTEMPTS);
        ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS);

        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", timeDealId);
        purchaseRequest.put("quantity", 1);

        for (int i = 0; i < ATTEMPTS; i++) {
            executor.submit(() -> {
                try {
                    Response response = givenWithToken(singleUserToken)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/shopping/time-deals/purchase");

                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - Exactly 2 should succeed (maxPerUser=2)
        log.info("MaxPerUser=2 test - Success: {} out of {}", successCount.get(), ATTEMPTS);

        assertThat(successCount.get())
                .isEqualTo(2)
                .withFailMessage("Expected exactly 2 successful purchases (maxPerUser=2), but got %d",
                        successCount.get());
    }

    @Test
    @Order(6)
    @DisplayName("6. Stress test: 300 users, 200 items, maxPerUser=1")
    void testHighLoadStress() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "No product available");

        final int STRESS_USERS = 300;
        final int STRESS_STOCK = 200;

        // Create stress test time-deal
        Map<String, Object> timeDealRequest = new HashMap<>();
        timeDealRequest.put("name", "Stress Test TimeDeal - " + generateTestId());
        timeDealRequest.put("productId", testProductId);
        timeDealRequest.put("dealPrice", 2000);
        timeDealRequest.put("stockQuantity", STRESS_STOCK);
        timeDealRequest.put("maxPerUser", 1);

        LocalDateTime now = LocalDateTime.now();
        timeDealRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        timeDealRequest.put("endAt", now.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/shopping/admin/time-deals");

        Long stressTimeDealId = createResponse.jsonPath().getLong("data.id");

        // Create additional users if needed
        List<String> stressUserTokens = new ArrayList<>(testUserTokens);
        int additionalUsersNeeded = STRESS_USERS - stressUserTokens.size();

        if (additionalUsersNeeded > 0) {
            log.info("Creating {} additional users for stress test", additionalUsersNeeded);
            ExecutorService userExecutor = Executors.newFixedThreadPool(20);
            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < additionalUsersNeeded; i++) {
                final int index = CONCURRENT_USERS + i;
                futures.add(userExecutor.submit(() -> {
                    String email = "td-stress-" + index + "-" + System.currentTimeMillis() + "@test.com";
                    try {
                        return createUserAndGetToken(email, "Test1234!", "Stress User " + index);
                    } catch (Exception e) {
                        return null;
                    }
                }));
            }

            for (Future<String> future : futures) {
                try {
                    String token = future.get(30, TimeUnit.SECONDS);
                    if (token != null) {
                        stressUserTokens.add(token);
                    }
                } catch (Exception ignored) {}
            }
            userExecutor.shutdown();
        }

        Assumptions.assumeTrue(stressUserTokens.size() >= STRESS_USERS,
                "Not enough users for stress test: " + stressUserTokens.size());

        log.info("Starting TimeDeal stress test: {} users, {} stock", STRESS_USERS, STRESS_STOCK);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(STRESS_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(STRESS_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(STRESS_USERS);

        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealId", stressTimeDealId);
        purchaseRequest.put("quantity", 1);

        for (int i = 0; i < STRESS_USERS; i++) {
            final String token = stressUserTokens.get(i);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    Response response = givenWithToken(token)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/shopping/time-deals/purchase");

                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(60, TimeUnit.SECONDS);
        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        doneLatch.await(180, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();

        log.info("Stress test completed in {}ms - {} successful purchases", duration, successCount.get());

        // Verify exactly STRESS_STOCK succeeded
        assertThat(successCount.get())
                .isEqualTo(STRESS_STOCK)
                .withFailMessage("Expected exactly %d successful purchases, but got %d",
                        STRESS_STOCK, successCount.get());
    }
}
