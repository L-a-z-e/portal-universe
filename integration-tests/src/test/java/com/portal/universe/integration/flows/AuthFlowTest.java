package com.portal.universe.integration.flows;

import com.portal.universe.integration.config.IntegrationTestBase;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Authentication Flow Integration Tests
 *
 * Tests the complete authentication lifecycle:
 * 1. User signup
 * 2. Kafka event verification
 * 3. User login
 * 4. Token-based API access
 * 5. Token refresh
 * 6. Logout
 */
@DisplayName("Authentication Flow Integration Tests")
@Tag("flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowTest extends IntegrationTestBase {

    private static final String KAFKA_USER_SIGNUP_TOPIC = "user-signup";
    private static String testUserEmail;
    private static String testUserPassword;
    private static String testUserToken;
    private static String testRefreshToken;

    @BeforeAll
    void initTestData() {
        testUserEmail = "authflow-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        testUserPassword = "TestPassword123!";
    }

    @Test
    @Order(1)
    @DisplayName("1. User signup should create user and publish Kafka event")
    void testUserSignup() {
        // Given
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", testUserEmail);
        signupData.put("password", testUserPassword);
        signupData.put("name", "Test User");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

        // Then
        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("success", is(true))
                .body("data.email", equalTo(testUserEmail));

        // Verify user was created
        assertThat(response.jsonPath().getString("data.id")).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. Signup should publish event to Kafka user-signup topic")
    void testSignupKafkaEventPublished() {
        // Given - Create Kafka consumer for user-signup topic
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(KAFKA_USER_SIGNUP_TOPIC)) {
            // Create a new user to trigger event
            String uniqueEmail = "kafka-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
            Map<String, String> signupData = new HashMap<>();
            signupData.put("email", uniqueEmail);
            signupData.put("password", "KafkaTest123!");
            signupData.put("name", "Kafka Test User");

            // When - Signup
            given()
                    .baseUri(AUTH_SERVICE_URL)
                    .contentType(ContentType.JSON)
                    .body(signupData)
                    .when()
                    .post("/api/v1/users/signup")
                    .then()
                    .statusCode(anyOf(is(200), is(201)));

            // Then - Verify Kafka event was published
            Optional<ConsumerRecord<String, String>> record = consumeMessage(
                    consumer,
                    KAFKA_USER_SIGNUP_TOPIC,
                    Duration.ofSeconds(10)
            );

            assertThat(record).isPresent();
            assertThat(record.get().value()).contains(uniqueEmail);
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. User login should return JWT access and refresh tokens")
    void testUserLogin() {
        // Given
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", testUserEmail);
        loginData.put("password", testUserPassword);

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("/api/v1/auth/login");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.accessToken", notNullValue())
                .body("data.tokenType", equalTo("Bearer"));

        // Store tokens for subsequent tests
        testUserToken = response.jsonPath().getString("data.accessToken");
        testRefreshToken = response.jsonPath().getString("data.refreshToken");

        assertThat(testUserToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("4. Authenticated request with valid token should succeed")
    void testAuthenticatedRequest() {
        // Given - User is logged in with valid token
        assertThat(testUserToken).isNotBlank();

        // When - Access protected resource
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testUserToken)
                .when()
                .get("/api/v1/auth/me");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.email", equalTo(testUserEmail));
    }

    @Test
    @Order(5)
    @DisplayName("5. Request without token should fail with 401")
    void testUnauthenticatedRequest() {
        // When - Access protected resource without token
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/auth/me");

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @Order(6)
    @DisplayName("6. Request with invalid token should fail with 401")
    void testInvalidTokenRequest() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get("/api/v1/auth/me");

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @Order(7)
    @DisplayName("7. Token refresh should return new access token")
    void testTokenRefresh() {
        // Skip if no refresh token available
        Assumptions.assumeTrue(testRefreshToken != null && !testRefreshToken.isBlank(),
                "Refresh token not available");

        // Given
        Map<String, String> refreshData = new HashMap<>();
        refreshData.put("refreshToken", testRefreshToken);

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(refreshData)
                .when()
                .post("/api/v1/auth/refresh");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.accessToken", notNullValue());

        String newToken = response.jsonPath().getString("data.accessToken");
        assertThat(newToken).isNotBlank();
        assertThat(newToken).isNotEqualTo(testUserToken);
    }

    @Test
    @Order(8)
    @DisplayName("8. Login with wrong password should fail with 401")
    void testLoginWrongPassword() {
        // Given
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", testUserEmail);
        loginData.put("password", "WrongPassword123!");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("/api/v1/auth/login");

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @Order(9)
    @DisplayName("9. Login with non-existent user should fail with 401")
    void testLoginNonExistentUser() {
        // Given
        Map<String, String> loginData = new HashMap<>();
        loginData.put("email", "nonexistent@test.com");
        loginData.put("password", "Password123!");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(loginData)
                .when()
                .post("/api/v1/auth/login");

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @Order(10)
    @DisplayName("10. Duplicate email signup should fail")
    void testDuplicateEmailSignup() {
        // Given - User already exists from test 1
        Map<String, String> signupData = new HashMap<>();
        signupData.put("email", testUserEmail);
        signupData.put("password", "AnotherPassword123!");
        signupData.put("name", "Duplicate User");

        // When
        Response response = given()
                .baseUri(AUTH_SERVICE_URL)
                .contentType(ContentType.JSON)
                .body(signupData)
                .when()
                .post("/api/v1/users/signup");

        // Then
        response.then()
                .statusCode(anyOf(is(400), is(409))); // Bad Request or Conflict
    }

    @Test
    @Order(11)
    @DisplayName("11. Admin user should have admin role")
    void testAdminRole() {
        // Given - Get admin token
        String adminToken = getAdminToken();

        // When - Access admin-only endpoint
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminToken)
                .when()
                .get("/api/v1/auth/me");

        // Then
        response.then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.roles", hasItem("ROLE_ADMIN"));
    }

    @Test
    @Order(12)
    @DisplayName("12. Regular user should not access admin endpoints")
    void testUserCannotAccessAdminEndpoint() {
        // Given - Regular user token
        assertThat(testUserToken).isNotBlank();

        // When - Try to access admin endpoint
        Response response = given()
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testUserToken)
                .when()
                .get("/api/v1/shopping/admin/products");

        // Then
        response.then()
                .statusCode(403); // Forbidden
    }
}
