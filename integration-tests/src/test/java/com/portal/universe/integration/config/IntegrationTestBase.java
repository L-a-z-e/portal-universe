package com.portal.universe.integration.config;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Base class for all integration tests.
 * Provides common utilities for API testing, authentication, and async operations.
 *
 * Note: This test class does NOT start a Spring context.
 * It tests against already running services.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    // Service URLs - configure based on your environment
    protected static final String GATEWAY_URL = "http://localhost:8080";
    protected static final String AUTH_SERVICE_URL = "http://localhost:8081";
    protected static final String SHOPPING_SERVICE_URL = "http://localhost:8083";
    protected static final String NOTIFICATION_SERVICE_URL = "http://localhost:8084";

    // Test user credentials
    protected static final String TEST_USER_EMAIL = "test@portal.com";
    protected static final String TEST_USER_PASSWORD = "Test1234!";
    protected static final String TEST_ADMIN_EMAIL = "admin@portal.com";
    protected static final String TEST_ADMIN_PASSWORD = "Admin1234!";

    // Cached auth tokens
    private static String cachedUserToken;
    private static String cachedAdminToken;
    private static long tokenExpiryTime = 0;

    // Gateway URL (can be overridden via system property)
    protected String gatewayUrl = System.getProperty("test.gateway.url", GATEWAY_URL);

    @BeforeAll
    static void setupRestAssured() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    void setUp() {
        // Reset token cache if expired (tokens valid for 1 hour, refresh at 50 minutes)
        if (System.currentTimeMillis() > tokenExpiryTime) {
            cachedUserToken = null;
            cachedAdminToken = null;
        }
    }

    // =======================================
    // Authentication Helpers
    // =======================================

    /**
     * Get or create an authenticated user token
     */
    protected String getUserToken() {
        if (cachedUserToken == null) {
            cachedUserToken = authenticate(TEST_USER_EMAIL, TEST_USER_PASSWORD);
            tokenExpiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(50);
        }
        return cachedUserToken;
    }

    /**
     * Get or create an authenticated admin token
     */
    protected String getAdminToken() {
        if (cachedAdminToken == null) {
            cachedAdminToken = authenticate(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD);
            tokenExpiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(50);
        }
        return cachedAdminToken;
    }

    /**
     * Authenticate and get JWT token
     */
    protected String authenticate(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Response response = RestAssured.given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/v1/auth/login");

        if (response.statusCode() == 200) {
            return response.jsonPath().getString("data.accessToken");
        }

        throw new RuntimeException("Authentication failed for user: " + email +
                " - Status: " + response.statusCode() + " - Body: " + response.body().asString());
    }

    /**
     * Create a new test user and return the token
     */
    protected String createUserAndGetToken(String email, String password, String name) {
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", email);
        signupData.put("password", password);
        signupData.put("name", name);

        Response signupResponse = RestAssured.given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

        if (signupResponse.statusCode() == 201 || signupResponse.statusCode() == 200) {
            return authenticate(email, password);
        }

        throw new RuntimeException("User creation failed: " + signupResponse.body().asString());
    }

    // =======================================
    // REST API Helpers
    // =======================================

    /**
     * Create an authenticated request to the gateway
     */
    protected RequestSpecification givenAuthenticatedUser() {
        return RestAssured.given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + getUserToken());
    }

    /**
     * Create an authenticated admin request to the gateway
     */
    protected RequestSpecification givenAuthenticatedAdmin() {
        return RestAssured.given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + getAdminToken());
    }

    /**
     * Create an unauthenticated request to the gateway
     */
    protected RequestSpecification givenUnauthenticated() {
        return RestAssured.given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON);
    }

    /**
     * Create a request with a specific token
     */
    protected RequestSpecification givenWithToken(String token) {
        return RestAssured.given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    // =======================================
    // Kafka Helpers
    // =======================================

    /**
     * Create a Kafka consumer for testing event publishing
     */
    protected KafkaConsumer<String, String> createKafkaConsumer(String... topics) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestContainersConfig.getKafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topics));
        return consumer;
    }

    /**
     * Wait for and consume a Kafka message with timeout
     */
    protected Optional<ConsumerRecord<String, String>> consumeMessage(
            KafkaConsumer<String, String> consumer,
            String expectedTopic,
            Duration timeout) {

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (record.topic().equals(expectedTopic)) {
                    return Optional.of(record);
                }
            }
        }
        return Optional.empty();
    }

    // =======================================
    // Concurrency Helpers
    // =======================================

    /**
     * Execute tasks concurrently and collect results
     */
    protected <T> List<T> executeConcurrently(
            int threadCount,
            Callable<T> task) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<T>> futures = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Wait for all threads to be ready
                return task.call();
            }));
        }

        // Wait for all threads to be ready, then start them simultaneously
        readyLatch.await();
        startLatch.countDown();

        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException e) {
                log.error("Task execution failed", e);
                results.add(null);
            }
        }

        executor.shutdown();
        return results;
    }

    /**
     * Count successful responses from concurrent execution
     */
    protected int countSuccessfulResponses(List<Response> responses, int expectedStatusCode) {
        return (int) responses.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.statusCode() == expectedStatusCode)
                .count();
    }

    // =======================================
    // Async Waiting Helpers
    // =======================================

    /**
     * Wait for a condition to be true with timeout
     */
    protected boolean waitFor(Callable<Boolean> condition, Duration timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                if (condition.call()) {
                    return true;
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.debug("Condition check failed", e);
            }
        }
        return false;
    }

    // =======================================
    // Test Data Helpers
    // =======================================

    /**
     * Generate unique test identifier
     */
    protected String generateTestId() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate unique email for test user
     */
    protected String generateTestEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@test.portal.com";
    }

    // =======================================
    // Response Assertion Helpers
    // =======================================

    /**
     * Extract data from ApiResponse wrapper
     */
    protected <T> T extractData(Response response, Class<T> dataClass) {
        return response.jsonPath().getObject("data", dataClass);
    }

    /**
     * Extract error code from ApiResponse
     */
    protected String extractErrorCode(Response response) {
        return response.jsonPath().getString("code");
    }

    /**
     * Assert response is successful ApiResponse
     */
    protected void assertSuccessResponse(Response response) {
        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        Boolean success = response.jsonPath().getBoolean("success");
        if (success == null || !success) {
            throw new AssertionError("Expected success response but got: " + response.body().asString());
        }
    }
}
