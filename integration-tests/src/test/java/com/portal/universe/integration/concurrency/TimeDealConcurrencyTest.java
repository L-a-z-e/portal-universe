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
 * 타임딜 동시성 테스트
 *
 * 고부하 동시성 환경에서의 타임딜 구매 검증:
 * - 재고 N개일 때 정확히 N건만 성공 (Over-selling 방지)
 * - 1인당 구매 제한 동시성 환경 적용 검증
 * - Redis Lua Script 원자성 검증
 */
@Slf4j
@DisplayName("타임딜 동시성 테스트")
@Tag("concurrency")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TimeDealConcurrencyTest extends IntegrationTestBase {

    private static final int TIMEDEAL_STOCK = 100;
    private static final int CONCURRENT_USERS = 150;
    private static final int MAX_PER_USER = 1;

    private static Long testTimeDealId;
    private static Long testTimeDealProductId;
    private static Long testProductId;
    private static List<String> testUserTokens = new ArrayList<>();

    /**
     * 타임딜 생성 요청 객체를 빌드한다.
     * TimeDealCreateRequest: name, description, startsAt, endsAt, products (TimeDealProductRequest 리스트)
     * TimeDealProductRequest: productId, dealPrice, dealQuantity, maxPerUser
     */
    private static Map<String, Object> buildTimeDealRequest(String name, Long productId,
                                                              int dealPrice, int dealQuantity, int maxPerUser,
                                                              LocalDateTime startsAt, LocalDateTime endsAt) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("startsAt", startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        request.put("endsAt", endsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Map<String, Object> product = new HashMap<>();
        product.put("productId", productId);
        product.put("dealPrice", dealPrice);
        product.put("dealQuantity", dealQuantity);
        product.put("maxPerUser", maxPerUser);

        request.put("products", List.of(product));
        return request;
    }

    @BeforeAll
    void setupTestData() {
        // 타임딜에 사용할 상품 조회
        Response productResponse = givenUnauthenticated()
                .when()
                .get("/api/v1/shopping/products");

        if (productResponse.statusCode() == 200) {
            List<Map<String, Object>> products = productResponse.jsonPath().getList("data.content");
            if (products != null && !products.isEmpty()) {
                testProductId = Long.valueOf(products.get(0).get("id").toString());
            }
        }

        Assumptions.assumeTrue(testProductId != null, "테스트에 사용할 상품이 없습니다");

        // 테스트 사용자 생성
        log.info("타임딜 동시성 테스트용 사용자 {}명 생성 중...", CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                String email = "timedeal-test-" + index + "-" + System.currentTimeMillis() + "@test.com";
                try {
                    return createUserAndGetToken(email, "SecurePw8!", "TimeDeal User " + index);
                } catch (Exception e) {
                    log.warn("사용자 {} 생성 실패: {}", index, e.getMessage());
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
                log.warn("사용자 토큰 획득 실패: {}", e.getMessage());
            }
        }

        executor.shutdown();
        log.info("테스트 사용자 {}명 생성 완료", testUserTokens.size());
    }

    @Test
    @Order(1)
    @DisplayName("1. 사전 설정: 관리자가 재고 제한 타임딜을 생성한다")
    void setupTimeDeal() {
        Assumptions.assumeTrue(testProductId != null, "사용할 상품이 없습니다");

        // Given - 중첩 products 구조로 요청 생성
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> timeDealRequest = buildTimeDealRequest(
                "Concurrency Test TimeDeal - " + generateTestId(),
                testProductId,
                5000,           // dealPrice
                TIMEDEAL_STOCK, // dealQuantity
                MAX_PER_USER,   // maxPerUser
                now,
                now.plusHours(2)
        );

        // When
        Response response = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/v1/shopping/admin/time-deals");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true));

        testTimeDealId = response.jsonPath().getLong("data.id");

        // 중첩 응답에서 timeDealProductId 추출
        List<Map<String, Object>> products = response.jsonPath().getList("data.products");
        if (products != null && !products.isEmpty()) {
            testTimeDealProductId = Long.valueOf(products.get(0).get("id").toString());
        }

        assertThat(testTimeDealId).isNotNull();
        assertThat(testTimeDealProductId).isNotNull();

        log.info("타임딜 생성 완료 - ID: {}, productId: {}, 재고: {}, 1인당 제한: {}",
                testTimeDealId, testTimeDealProductId, TIMEDEAL_STOCK, MAX_PER_USER);
    }

    @Test
    @Order(2)
    @DisplayName("2. 150명 동시 구매 시 재고 100개면 정확히 100건만 성공해야 한다")
    void testConcurrentTimeDealPurchase() throws InterruptedException {
        Assumptions.assumeTrue(testTimeDealProductId != null, "타임딜 상품이 생성되지 않았습니다");
        Assumptions.assumeTrue(testUserTokens.size() >= CONCURRENT_USERS,
                "테스트 사용자 부족: " + testUserTokens.size());

        log.info("타임딜 동시 구매 테스트 시작: 사용자 {}명, 재고 {}개",
                CONCURRENT_USERS, TIMEDEAL_STOCK);

        // Given
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger outOfStockCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);

        // TimeDealPurchaseRequest는 timeDealProductId 사용 (timeDealId 아님)
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealProductId", testTimeDealProductId);
        purchaseRequest.put("quantity", 1);

        // When - 모든 사용자가 동시에 구매 시도
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
                            .post("/api/v1/shopping/time-deals/purchase");

                    int status = response.statusCode();
                    // ApiResponse wrapper에서 에러 코드 추출
                    String code = response.jsonPath().getString("error.code");

                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                        log.debug("사용자 {} 구매 성공", userId);
                    } else if ("S704".equals(code) || "S705".equals(code)) {
                        // S704: 재고 소진, S705: 재고 부족
                        outOfStockCount.incrementAndGet();
                        log.debug("사용자 {} - 재고 소진", userId);
                    } else {
                        errorCount.incrementAndGet();
                        log.debug("사용자 {} - 기타 실패: {} {}", userId, status, code);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("사용자 {} 오류: {}", userId, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드 준비 대기
        readyLatch.await(30, TimeUnit.SECONDS);

        // 동시 시작
        log.info("사용자 {}명 준비 완료, 동시 구매 시작...", CONCURRENT_USERS);
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // 완료 대기
        boolean completed = doneLatch.await(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        executor.shutdown();

        // Then
        log.info("동시성 테스트 완료: {}ms 소요", duration);
        log.info("결과 - 성공: {}, 재고 소진: {}, 오류: {}",
                successCount.get(), outOfStockCount.get(), errorCount.get());

        assertThat(completed).isTrue().withFailMessage("테스트 시간 초과");

        // 핵심 검증: 정확히 TIMEDEAL_STOCK 수만큼만 성공해야 함 (Over-selling 금지)
        assertThat(successCount.get())
                .isEqualTo(TIMEDEAL_STOCK)
                .withFailMessage("성공 건수가 %d이어야 하지만 %d입니다",
                        TIMEDEAL_STOCK, successCount.get());

        // 나머지 사용자는 재고 소진으로 실패해야 함
        assertThat(outOfStockCount.get())
                .isEqualTo(CONCURRENT_USERS - TIMEDEAL_STOCK)
                .withFailMessage("재고 소진 실패 건수가 %d이어야 하지만 %d입니다",
                        CONCURRENT_USERS - TIMEDEAL_STOCK, outOfStockCount.get());
    }

    @Test
    @Order(3)
    @DisplayName("3. 동시 구매 후 타임딜 재고가 정확히 0이어야 한다")
    void testTimeDealStockZero() {
        Assumptions.assumeTrue(testTimeDealId != null, "타임딜이 생성되지 않았습니다");

        // When
        Response response = givenAuthenticatedUser()
                .when()
                .get("/api/v1/shopping/time-deals/" + testTimeDealId);

        // Then - 중첩 products의 remainingQuantity 검증
        response.then()
                .statusCode(200)
                .body("success", is(true));

        List<Map<String, Object>> products = response.jsonPath().getList("data.products");
        if (products != null && !products.isEmpty()) {
            int remainingStock = Integer.parseInt(products.get(0).get("remainingQuantity").toString());
            log.info("타임딜 잔여 재고: {}", remainingStock);
            assertThat(remainingStock).isZero();
        }
    }

    @Test
    @Order(4)
    @DisplayName("4. 동일 사용자는 2회 이상 구매할 수 없다 (1인당 제한 검증)")
    void testMaxPerUserEnforcement() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "사용할 상품이 없습니다");

        // 이 테스트용 새 타임딜 생성
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> timeDealRequest = buildTimeDealRequest(
                "MaxPerUser Test TimeDeal - " + generateTestId(),
                testProductId,
                3000,  // dealPrice
                20,    // dealQuantity
                1,     // maxPerUser
                now,
                now.plusHours(1)
        );

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/v1/shopping/admin/time-deals");

        // 중첩 응답에서 timeDealProductId 추출
        List<Map<String, Object>> products = createResponse.jsonPath().getList("data.products");
        Long maxPerUserProductId = null;
        if (products != null && !products.isEmpty()) {
            maxPerUserProductId = Long.valueOf(products.get(0).get("id").toString());
        }

        Assumptions.assumeTrue(maxPerUserProductId != null, "타임딜 상품이 생성되지 않았습니다");

        // 단일 사용자가 동시에 5회 구매 시도
        String singleUserToken = testUserTokens.get(0);
        final int CONCURRENT_ATTEMPTS = 5;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger limitExceededCount = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(CONCURRENT_ATTEMPTS);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);

        final Long finalProductId = maxPerUserProductId;
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealProductId", finalProductId);
        purchaseRequest.put("quantity", 1);

        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            executor.submit(() -> {
                try {
                    Response response = givenWithToken(singleUserToken)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/v1/shopping/time-deals/purchase");

                    int status = response.statusCode();
                    String code = response.jsonPath().getString("error.code");

                    if (status == 200 || status == 201) {
                        successCount.incrementAndGet();
                    } else if ("S706".equals(code)) {
                        // 1인당 구매 제한 초과
                        limitExceededCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 정확히 1건만 성공해야 함
        log.info("1인당 제한 테스트 - 성공: {}, 제한 초과: {}",
                successCount.get(), limitExceededCount.get());

        assertThat(successCount.get())
                .isEqualTo(1)
                .withFailMessage("성공 건수가 1이어야 하지만 %d입니다", successCount.get());

        assertThat(limitExceededCount.get())
                .isEqualTo(CONCURRENT_ATTEMPTS - 1)
                .withFailMessage("제한 초과 건수가 %d이어야 하지만 %d입니다",
                        CONCURRENT_ATTEMPTS - 1, limitExceededCount.get());
    }

    @Test
    @Order(5)
    @DisplayName("5. maxPerUser=2일 때 동일 사용자가 정확히 2건까지 구매 가능해야 한다")
    void testMaxPerUserTwo() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "사용할 상품이 없습니다");

        // maxPerUser=2 타임딜 생성
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> timeDealRequest = buildTimeDealRequest(
                "MaxPerUser2 Test - " + generateTestId(),
                testProductId,
                4000,  // dealPrice
                50,    // dealQuantity
                2,     // maxPerUser = 2
                now,
                now.plusHours(1)
        );

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/v1/shopping/admin/time-deals");

        // 중첩 응답에서 timeDealProductId 추출
        List<Map<String, Object>> products = createResponse.jsonPath().getList("data.products");
        Long timeDealProductId = null;
        if (products != null && !products.isEmpty()) {
            timeDealProductId = Long.valueOf(products.get(0).get("id").toString());
        }

        Assumptions.assumeTrue(timeDealProductId != null, "타임딜 상품이 생성되지 않았습니다");

        // 단일 사용자가 5회 구매 시도
        String singleUserToken = testUserTokens.get(1);
        final int ATTEMPTS = 5;

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(ATTEMPTS);
        ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS);

        final Long finalProductId = timeDealProductId;
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealProductId", finalProductId);
        purchaseRequest.put("quantity", 1);

        for (int i = 0; i < ATTEMPTS; i++) {
            executor.submit(() -> {
                try {
                    Response response = givenWithToken(singleUserToken)
                            .body(purchaseRequest)
                            .when()
                            .post("/api/v1/shopping/time-deals/purchase");

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

        // Then - 정확히 2건 성공해야 함 (maxPerUser=2)
        log.info("maxPerUser=2 테스트 - 성공: {} / 시도: {}", successCount.get(), ATTEMPTS);

        assertThat(successCount.get())
                .isEqualTo(2)
                .withFailMessage("성공 건수가 2(maxPerUser=2)이어야 하지만 %d입니다",
                        successCount.get());
    }

    @Test
    @Order(6)
    @DisplayName("6. 부하 테스트: 400명 동시 요청, 재고 200개, 1인당 1개 제한")
    void testHighLoadStress() throws InterruptedException {
        Assumptions.assumeTrue(testProductId != null, "사용할 상품이 없습니다");

        final int STRESS_USERS = 400;
        final int STRESS_STOCK = 200;

        // 부하 테스트용 타임딜 생성
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> timeDealRequest = buildTimeDealRequest(
                "Stress Test TimeDeal - " + generateTestId(),
                testProductId,
                2000,        // dealPrice
                STRESS_STOCK, // dealQuantity
                1,           // maxPerUser
                now,
                now.plusHours(1)
        );

        Response createResponse = givenAuthenticatedAdmin()
                .body(timeDealRequest)
                .when()
                .post("/api/v1/shopping/admin/time-deals");

        // 중첩 응답에서 timeDealProductId 추출
        List<Map<String, Object>> products = createResponse.jsonPath().getList("data.products");
        Long stressProductId = null;
        if (products != null && !products.isEmpty()) {
            stressProductId = Long.valueOf(products.get(0).get("id").toString());
        }

        Assumptions.assumeTrue(stressProductId != null, "타임딜 상품이 생성되지 않았습니다");

        // 부족한 사용자 추가 생성
        List<String> stressUserTokens = new ArrayList<>(testUserTokens);
        int additionalUsersNeeded = STRESS_USERS - stressUserTokens.size();

        if (additionalUsersNeeded > 0) {
            log.info("부하 테스트용 추가 사용자 {}명 생성 중", additionalUsersNeeded);
            ExecutorService userExecutor = Executors.newFixedThreadPool(20);
            List<Future<String>> futures = new ArrayList<>();

            for (int i = 0; i < additionalUsersNeeded; i++) {
                final int index = CONCURRENT_USERS + i;
                futures.add(userExecutor.submit(() -> {
                    String email = "td-stress-" + index + "-" + System.currentTimeMillis() + "@test.com";
                    try {
                        return createUserAndGetToken(email, "SecurePw8!", "Stress User " + index);
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
                "부하 테스트 사용자 부족: " + stressUserTokens.size());

        log.info("타임딜 부하 테스트 시작: 사용자 {}명, 재고 {}개", STRESS_USERS, STRESS_STOCK);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(STRESS_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(STRESS_USERS);

        ExecutorService executor = Executors.newFixedThreadPool(STRESS_USERS);

        final Long finalStressProductId = stressProductId;
        Map<String, Object> purchaseRequest = new HashMap<>();
        purchaseRequest.put("timeDealProductId", finalStressProductId);
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
                            .post("/api/v1/shopping/time-deals/purchase");

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

        log.info("부하 테스트 완료: {}ms 소요 - 성공 {}건", duration, successCount.get());

        // 핵심 검증: 정확히 STRESS_STOCK 수만큼만 성공해야 함
        assertThat(successCount.get())
                .isEqualTo(STRESS_STOCK)
                .withFailMessage("성공 건수가 %d이어야 하지만 %d입니다",
                        STRESS_STOCK, successCount.get());
    }
}