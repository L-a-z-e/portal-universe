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
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Coupon Concurrency Tests
 *
 * Tests for first-come-first-served coupon issuance under high concurrency:
 * - Exactly N coupons issued when N available (no over-issuance)
 * - No duplicate issuance to same user
 * - Redis Lua script atomicity verification
 */
@Slf4j
@DisplayName("Coupon Concurrency Tests")
@Tag("concurrency")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CouponConcurrencyTest extends IntegrationTestBase {

    private static final int COUPON_QUANTITY = 50;
    private static final int CONCURRENT_USERS = 100;

    private static Long testCouponId;
    private static List<String> testUserTokens = new ArrayList<>();

    @BeforeAll
    void setupTestUsers() {
        log.info("Creating {} test users for concurrency test...", CONCURRENT_USERS);

        // Create test users in parallel
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String email = "coupon-test-" + index + "-" + System.currentTimeMillis() + "@test.com";
                try {
                    return createUserAndGetToken(email, "Test1234!", "Test User " + index);
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
    @DisplayName("1. Setup: Admin creates limited quantity coupon")
    void setupCoupon() {
        // Given
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Concurrency Test Coupon - " + generateTestId());
        couponRequest.put("code", "CONCURRENT" + System.currentTimeMillis());
        couponRequest.put("discountType", "PERCENTAGE");
        couponRequest.put("discountValue", 20);
        couponRequest.put("totalQuantity", COUPON_QUANTITY);
        couponRequest.put("maxDiscountAmount", 10000);

        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // When
        Response response = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/v1/shopping/admin/coupons");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true));

        testCouponId = response.jsonPath().getLong("data.id");
        assertThat(testCouponId).isNotNull();

        log.info("Created test coupon with ID: {}, quantity: {}", testCouponId, COUPON_QUANTITY);
    }

    @Test
    @Order(2)
    @DisplayName("2. 100 concurrent users request 50 coupons - exactly 50 should succeed")
    void testConcurrentCouponIssuance() throws InterruptedException {
        // Skip if setup failed
        Assumptions.assumeTrue(testCouponId != null, "No coupon created");
        Assumptions.assumeTrue(testUserTokens.size() >= CONCURRENT_USERS,
                "Not enough test users: " + testUserTokens.size());

        log.info("Starting concurrent coupon issuance test: {} users, {} coupons",
                CONCURRENT_USERS, COUPON_QUANTITY);

        // Given
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);

        // When - All users request coupon simultaneously
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final String token = testUserTokens.get(i);
            final int userId = i;

            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await(); // Wait for signal

                    Response response = givenWithToken(token)
                            .when()
                            .post("/api/v1/shopping/coupons/" + testCouponId + "/issue");

                    int status = response.statusCode();
                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                        log.debug("User {} succeeded", userId);
                    } else if (status == 400 || status == 409) {
                        // Expected failure: coupon exhausted or already issued
                        failCount.incrementAndGet();
                        log.debug("User {} failed (expected): {}", userId,
                                response.jsonPath().getString("code"));
                    } else {
                        errorCount.incrementAndGet();
                        log.warn("User {} unexpected status: {}", userId, status);
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
        log.info("All {} users ready, starting simultaneous requests...", CONCURRENT_USERS);
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();

        // Then
        log.info("Concurrency test completed in {}ms", duration);
        log.info("Results - Success: {}, Fail: {}, Error: {}",
                successCount.get(), failCount.get(), errorCount.get());

        assertThat(completed).isTrue().withFailMessage("Test timed out");
        assertThat(errorCount.get()).isZero().withFailMessage("Unexpected errors occurred");

        // CRITICAL: Exactly COUPON_QUANTITY should succeed (no over-issuance)
        assertThat(successCount.get())
                .isEqualTo(COUPON_QUANTITY)
                .withFailMessage("Expected exactly %d successful issues, but got %d",
                        COUPON_QUANTITY, successCount.get());

        // Remaining users should fail
        assertThat(failCount.get())
                .isEqualTo(CONCURRENT_USERS - COUPON_QUANTITY)
                .withFailMessage("Expected %d failures, but got %d",
                        CONCURRENT_USERS - COUPON_QUANTITY, failCount.get());
    }

    @Test
    @Order(3)
    @DisplayName("3. Verify coupon stock is exactly zero after concurrent issuance")
    void testCouponStockZero() {
        // Skip if no coupon
        Assumptions.assumeTrue(testCouponId != null, "No coupon created");

        // When
        Response response = givenAuthenticatedAdmin()
                .when()
                .get("/api/v1/shopping/admin/coupons/" + testCouponId);

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true));

        int remainingQuantity = response.jsonPath().getInt("data.remainingQuantity");
        int issuedCount = response.jsonPath().getInt("data.issuedCount");

        log.info("Coupon status - Remaining: {}, Issued: {}", remainingQuantity, issuedCount);

        assertThat(remainingQuantity).isZero();
        assertThat(issuedCount).isEqualTo(COUPON_QUANTITY);
    }

    @Test
    @Order(4)
    @DisplayName("4. Additional issuance after exhaustion should fail with S602")
    void testIssuanceAfterExhaustion() {
        // Skip if no coupon
        Assumptions.assumeTrue(testCouponId != null, "No coupon created");

        // Create a new user who didn't participate in concurrent test
        String newUserEmail = generateTestEmail();
        String newUserToken = createUserAndGetToken(newUserEmail, "Test1234!", "New User");

        // When - Try to issue exhausted coupon
        Response response = givenWithToken(newUserToken)
                .when()
                .post("/api/v1/shopping/coupons/" + testCouponId + "/issue");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(409)))
                .body("success", is(false))
                .body("code", equalTo("S602")); // COUPON_EXHAUSTED
    }

    @Test
    @Order(5)
    @DisplayName("5. Test same user cannot issue twice (duplicate prevention)")
    void testDuplicateIssuePrevention() throws InterruptedException {
        // Create a fresh coupon with quantity 10
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Duplicate Test Coupon - " + generateTestId());
        couponRequest.put("code", "DUPTEST" + System.currentTimeMillis());
        couponRequest.put("discountType", "FIXED");
        couponRequest.put("discountValue", 1000);
        couponRequest.put("totalQuantity", 10);

        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/v1/shopping/admin/coupons");

        Long dupCouponId = createResponse.jsonPath().getLong("data.id");

        // Single user tries to issue same coupon 10 times concurrently
        String singleUserToken = testUserTokens.get(0);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    Response response = givenWithToken(singleUserToken)
                            .when()
                            .post("/api/v1/shopping/coupons/" + dupCouponId + "/issue");

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

        // Then - Only 1 should succeed (no duplicate)
        assertThat(successCount.get())
                .isEqualTo(1)
                .withFailMessage("Expected exactly 1 successful issue, but got %d", successCount.get());

        log.info("Duplicate prevention test passed - {} success out of 10 attempts", successCount.get());
    }

    @Test
    @Order(6)
    @DisplayName("6. Stress test: 200 users, 100 coupons")
    void testHighLoadStress() throws InterruptedException {
        final int STRESS_USERS = 200;
        final int STRESS_COUPONS = 100;

        // Create stress test coupon
        Map<String, Object> couponRequest = new HashMap<>();
        couponRequest.put("name", "Stress Test Coupon - " + generateTestId());
        couponRequest.put("code", "STRESS" + System.currentTimeMillis());
        couponRequest.put("discountType", "PERCENTAGE");
        couponRequest.put("discountValue", 15);
        couponRequest.put("totalQuantity", STRESS_COUPONS);

        LocalDateTime now = LocalDateTime.now();
        couponRequest.put("startAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        couponRequest.put("endAt", now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Response createResponse = givenAuthenticatedAdmin()
                .body(couponRequest)
                .when()
                .post("/api/v1/shopping/admin/coupons");

        Long stressCouponId = createResponse.jsonPath().getLong("data.id");

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
                    String email = "stress-" + index + "-" + System.currentTimeMillis() + "@test.com";
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

        // Skip if not enough users
        Assumptions.assumeTrue(stressUserTokens.size() >= STRESS_USERS,
                "Not enough users for stress test: " + stressUserTokens.size());

        log.info("Starting stress test: {} users, {} coupons", STRESS_USERS, STRESS_COUPONS);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(STRESS_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(STRESS_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(STRESS_USERS);

        for (int i = 0; i < STRESS_USERS; i++) {
            final String token = stressUserTokens.get(i);
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    Response response = givenWithToken(token)
                            .when()
                            .post("/api/v1/shopping/coupons/" + stressCouponId + "/issue");

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
        doneLatch.await(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();

        log.info("Stress test completed in {}ms - {} successful issues", duration, successCount.get());

        // Verify exactly STRESS_COUPONS succeeded
        assertThat(successCount.get())
                .isEqualTo(STRESS_COUPONS)
                .withFailMessage("Expected exactly %d successful issues, but got %d",
                        STRESS_COUPONS, successCount.get());
    }
}
