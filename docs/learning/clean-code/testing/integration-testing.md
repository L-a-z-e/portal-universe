# 통합 테스트 (Integration Testing)

## 학습 목표

- 통합 테스트의 정의와 단위 테스트와의 차이점 이해
- Testcontainers를 사용한 실제 데이터베이스 환경 구성
- @SpringBootTest를 활용한 Spring 컨텍스트 통합 테스트
- 동시성 문제를 검증하는 테스트 작성법 습득
- 통합 테스트 설계 및 최적화 전략 학습

## 테스트 피라미드에서의 위치

```
        /\
       /  \      E2E Tests (소수, 느림, 비싸다)
      /____\
     /      \    Integration Tests (중간, 적당한 속도) ← 여기!
    /________\
   /          \  Unit Tests (다수, 빠름, 저렴하다)
  /__________\
```

**통합 테스트의 특징:**
- 여러 컴포넌트 간의 상호작용 검증
- 실제 데이터베이스, 메시징 시스템 등과 통합
- 단위 테스트보다 느리지만 E2E보다 빠름
- 인프라 의존성 포함

## 통합 테스트란?

통합 테스트는 여러 모듈이나 컴포넌트가 함께 올바르게 작동하는지 검증하는 테스트입니다.

**단위 테스트 vs 통합 테스트:**

| 구분 | 단위 테스트 | 통합 테스트 |
|------|------------|------------|
| 범위 | 단일 클래스/메서드 | 여러 컴포넌트 조합 |
| 의존성 | Mock으로 격리 | 실제 의존성 사용 |
| 속도 | 매우 빠름 (ms) | 중간 속도 (초) |
| 환경 | 메모리만 사용 | DB, 네트워크 등 필요 |
| 목적 | 로직 정확성 | 통합 정확성 |

**통합 테스트가 필요한 경우:**
- DB 쿼리 동작 검증 (JPA, QueryDSL)
- 트랜잭션 경계 검증
- 동시성 제어 검증 (Lock, Isolation)
- Spring Security 설정 검증
- 캐시 동작 검증
- 메시징 시스템 통합 검증

## @SpringBootTest - Spring 통합 테스트

### 기본 구조

```java
@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createAndFindUser() {
        // given
        UserRequest request = new UserRequest("john@example.com", "John Doe");

        // when
        UserResponse created = userService.create(request);
        UserResponse found = userService.findById(created.id());

        // then
        assertThat(found.email()).isEqualTo("john@example.com");
    }
}
```

### WebEnvironment 옵션

```java
// 1. MOCK (기본값): MockMvc 사용, 서버 실행 안 함
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ControllerMockTest {
    @Autowired
    private MockMvc mockMvc;
}

// 2. RANDOM_PORT: 실제 서버 실행 (랜덤 포트)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;
}

// 3. DEFINED_PORT: 지정된 포트로 서버 실행
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ControllerDefinedPortTest { }

// 4. NONE: 웹 환경 없음 (Service 계층만 테스트)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ServiceOnlyTest { }
```

### @TestConfiguration

```java
// 테스트용 추가 설정
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary // 운영 Bean을 Override
    public EmailService emailService() {
        return new MockEmailService();
    }
}

// 테스트에서 사용
@SpringBootTest
@Import(TestConfig.class)
class UserServiceTest { }
```

## Testcontainers - 실제 환경 구성

Testcontainers는 Docker 컨테이너를 사용하여 실제 데이터베이스, Redis, Kafka 등을 테스트 환경에서 실행하는 라이브러리입니다.

### 의존성 추가

```gradle
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
testImplementation 'org.testcontainers:mysql:1.19.3'
```

### Portal Universe의 IntegrationTest 베이스 클래스

**위치:** `services/shopping-service/src/test/java/.../IntegrationTest.java`

```java
/**
 * 모든 통합 테스트를 위한 추상 베이스 클래스입니다.
 *
 * 주요 역할:
 * 1. Testcontainers를 사용하여 테스트용 MySQL 인스턴스 실행
 * 2. 외부 시스템(Config Server, Eureka) 의존성 비활성화
 * 3. 일관된 테스트 환경 제공
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = IntegrationTest.DataSourceInitializer.class)
public abstract class IntegrationTest {

    /**
     * 모든 테스트 클래스에서 공유하는 MySQL 컨테이너
     * static으로 선언하여 한 번만 시작
     */
    private static final MySQLContainer<?> mySQLContainer;

    static {
        mySQLContainer = new MySQLContainer<>("mysql:8.0");
        mySQLContainer.start();
    }

    /**
     * Testcontainer의 동적 설정을 Spring에 주입
     */
    public static class DataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // 외부 시스템 비활성화
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.discovery.enabled=false",
                    "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",

                    // JPA 설정
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false",

                    // Testcontainers 동적 DB 설정
                    "spring.datasource.url=" + mySQLContainer.getJdbcUrl(),
                    "spring.datasource.username=" + mySQLContainer.getUsername(),
                    "spring.datasource.password=" + mySQLContainer.getPassword()
            );
        }
    }
}
```

**사용 방법:**

```java
// 모든 통합 테스트는 IntegrationTest를 상속
class InventoryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void test() {
        // 실제 MySQL DB를 사용한 테스트
    }
}
```

**장점:**
- ✅ 실제 DB 환경과 동일한 환경에서 테스트
- ✅ H2 같은 In-memory DB의 차이로 인한 문제 방지
- ✅ Docker만 있으면 어디서든 실행 가능
- ✅ 테스트 간 완전한 격리 (create-drop)

### 다른 Testcontainers 예제

#### PostgreSQL

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

@DynamicPropertySource
static void registerPgProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

#### Redis

```java
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

@DynamicPropertySource
static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", redis::getFirstMappedPort);
}
```

#### Kafka

```java
@Container
static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

@DynamicPropertySource
static void registerKafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
}
```

## Portal Universe 통합 테스트 분석

### 예제 1: 기본적인 통합 테스트

**위치:** `services/shopping-service/src/test/java/.../InventoryServiceIntegrationTest.java`

```java
class InventoryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    @DisplayName("재고를 초기화하고 조회할 수 있다")
    void initializeAndGetInventory() {
        // given
        Long productId = 1001L;
        int initialStock = 100;

        // when
        InventoryResponse response = inventoryService.initializeInventory(
                productId, initialStock, "test-admin");

        // then
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.availableQuantity()).isEqualTo(initialStock);
        assertThat(response.reservedQuantity()).isEqualTo(0);
        assertThat(response.totalQuantity()).isEqualTo(initialStock);

        // 조회 확인 (DB에 실제로 저장되었는지)
        InventoryResponse fetched = inventoryService.getInventory(productId);
        assertThat(fetched.availableQuantity()).isEqualTo(initialStock);
    }

    @Test
    @DisplayName("재고를 예약하면 가용 재고가 감소하고 예약 재고가 증가한다")
    void reserveStockUpdatesQuantities() {
        // given
        Long productId = 1003L;
        inventoryService.initializeInventory(productId, 100, "test-admin");

        // when
        InventoryResponse response = inventoryService.reserveStock(
                productId, 30, "ORDER", "ORD-001", "test-user");

        // then
        assertThat(response.availableQuantity()).isEqualTo(70);
        assertThat(response.reservedQuantity()).isEqualTo(30);
        assertThat(response.totalQuantity()).isEqualTo(100);
    }
}
```

**학습 포인트:**
- Service와 Repository가 통합되어 동작하는지 검증
- 실제 DB에 데이터가 저장되고 조회되는지 확인
- 트랜잭션이 올바르게 적용되는지 검증

### 예제 2: 동시성 테스트

```java
@Test
@DisplayName("동시에 여러 요청이 재고를 예약할 때 데이터 일관성이 유지된다")
void concurrentReservationsMaintainConsistency() throws InterruptedException {
    // given
    Long productId = 1010L;
    int initialStock = 100;
    inventoryService.initializeInventory(productId, initialStock, "test-admin");

    int numberOfThreads = 10;
    int reservationPerThread = 5; // 각 스레드가 5개씩 예약 (총 50개)

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // when
    for (int i = 0; i < numberOfThreads; i++) {
        final int threadNum = i;
        executor.submit(() -> {
            try {
                inventoryService.reserveStock(
                        productId, reservationPerThread,
                        "ORDER", "ORD-CONCURRENT-" + threadNum, "test-user");
                successCount.incrementAndGet();
            } catch (CustomBusinessException e) {
                if (e.getErrorCode() == ShoppingErrorCode.INSUFFICIENT_STOCK) {
                    failCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executor.shutdown();

    // then
    InventoryResponse finalState = inventoryService.getInventory(productId);

    // 모든 요청이 성공했으므로 50개가 예약되어야 함
    assertThat(successCount.get()).isEqualTo(numberOfThreads);
    assertThat(finalState.availableQuantity())
            .isEqualTo(initialStock - (numberOfThreads * reservationPerThread));
    assertThat(finalState.reservedQuantity())
            .isEqualTo(numberOfThreads * reservationPerThread);
}
```

**학습 포인트:**
- `ExecutorService`로 멀티스레드 환경 시뮬레이션
- `CountDownLatch`로 모든 스레드 작업 완료 대기
- `AtomicInteger`로 Thread-safe 카운팅
- DB Lock이 올바르게 동작하는지 검증

### 예제 3: 재고 부족 시나리오 테스트

```java
@Test
@DisplayName("가용 재고보다 많은 동시 예약 요청 시 일부만 성공한다")
void concurrentReservationsWithInsufficientStock() throws InterruptedException {
    // given
    Long productId = 1011L;
    int initialStock = 10;
    inventoryService.initializeInventory(productId, initialStock, "test-admin");

    int numberOfThreads = 15;
    int reservationPerThread = 1; // 총 15개 시도, 10개만 성공해야 함

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    // when
    for (int i = 0; i < numberOfThreads; i++) {
        final int threadNum = i;
        executor.submit(() -> {
            try {
                inventoryService.reserveStock(
                        productId, reservationPerThread,
                        "ORDER", "ORD-OVERFLOW-" + threadNum, "test-user");
                successCount.incrementAndGet();
            } catch (CustomBusinessException e) {
                if (e.getErrorCode() == ShoppingErrorCode.INSUFFICIENT_STOCK) {
                    failCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await();
    executor.shutdown();

    // then
    InventoryResponse finalState = inventoryService.getInventory(productId);

    // 10개만 성공, 5개 실패
    assertThat(successCount.get()).isEqualTo(10);
    assertThat(failCount.get()).isEqualTo(5);
    assertThat(finalState.availableQuantity()).isEqualTo(0);
    assertThat(finalState.reservedQuantity()).isEqualTo(10);
}
```

**학습 포인트:**
- Race Condition 시나리오 검증
- 예외 처리가 동시성 환경에서도 올바른지 확인
- 데이터 무결성 검증 (Over-selling 방지)

## 동시성 테스트 패턴

### 1. ExecutorService + CountDownLatch

```java
@Test
void concurrentTest() throws InterruptedException {
    int threadCount = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                // 테스트할 동작
                service.someOperation();
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(10, TimeUnit.SECONDS); // 타임아웃 설정
    executor.shutdown();

    // 검증
}
```

### 2. CyclicBarrier - 동시 시작 보장

```java
@Test
void simultaneousStart() throws Exception {
    int threadCount = 10;
    CyclicBarrier barrier = new CyclicBarrier(threadCount);
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                barrier.await(); // 모든 스레드가 여기서 대기
                // 모든 스레드가 동시에 시작
                service.someOperation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
}
```

### 3. CompletableFuture - 비동기 작업 테스트

```java
@Test
void asyncOperationTest() {
    List<CompletableFuture<Void>> futures = IntStream.range(0, 100)
            .mapToObj(i -> CompletableFuture.runAsync(() ->
                    service.someOperation()))
            .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

    // 검증
}
```

## 트랜잭션 테스트

### 1. @Transactional 롤백 테스트

```java
@SpringBootTest
@Transactional // 기본적으로 롤백됨
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser() {
        userService.create(new UserRequest("john@example.com"));

        // 테스트 내에서는 조회 가능
        assertThat(userRepository.findAll()).hasSize(1);

        // 테스트 종료 후 자동 롤백
    }
}
```

### 2. @Commit - 실제 커밋 테스트

```java
@Test
@Commit // 또는 @Rollback(false)
void createUserWithCommit() {
    userService.create(new UserRequest("john@example.com"));
    // 실제로 DB에 커밋됨
}
```

### 3. Propagation 테스트

```java
@Test
void testTransactionPropagation() {
    // REQUIRED: 기존 트랜잭션 사용, 없으면 새로 생성
    outerService.doInTransaction(() -> {
        innerService.doInRequiredTransaction(); // 같은 트랜잭션
    });

    // REQUIRES_NEW: 항상 새 트랜잭션 생성
    outerService.doInTransaction(() -> {
        innerService.doInRequiresNewTransaction(); // 새 트랜잭션
        throw new RuntimeException(); // outer 롤백, inner는 커밋됨
    });
}
```

## MockMvc를 사용한 Controller 통합 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProduct() throws Exception {
        ProductRequest request = new ProductRequest("Laptop", 1500000);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Laptop"))
                .andExpect(jsonPath("$.data.price").value(1500000));
    }

    @Test
    void getProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getProductNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

## 테스트 데이터 관리

### 1. @Sql - SQL 스크립트 실행

```java
@Test
@Sql("/test-data/products.sql") // 테스트 전 실행
@Sql(scripts = "/test-data/cleanup.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD) // 테스트 후 실행
void testWithPreloadedData() {
    List<Product> products = productRepository.findAll();
    assertThat(products).hasSize(10);
}
```

### 2. @DirtiesContext - 컨텍스트 재생성

```java
@Test
@DirtiesContext // 이 테스트 후 Spring 컨텍스트 재생성
void testThatModifiesContext() {
    // 공유 빈의 상태를 변경하는 테스트
}
```

### 3. TestEntityManager (JPA)

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail() {
        // given
        User user = new User("john@example.com", "John");
        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByEmail("john@example.com");

        // then
        assertThat(found).isPresent();
    }
}
```

## 모범 사례

### 1. 베이스 클래스 활용

```java
// ✅ 공통 설정을 베이스 클래스로 추출
@SpringBootTest
@ContextConfiguration(initializers = BaseIntegrationTest.Initializer.class)
public abstract class BaseIntegrationTest {
    // Testcontainers 설정
    // 공통 Helper 메서드
}

// 실제 테스트
class ProductServiceTest extends BaseIntegrationTest {
    // 테스트에만 집중
}
```

### 2. 테스트 컨테이너 재사용

```java
// ✅ static으로 선언하여 모든 테스트 클래스가 공유
private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

static {
    mysql.start(); // 한 번만 시작
}
```

### 3. 테스트 격리

```java
// ✅ @DirtiesContext 또는 @Transactional로 격리
@SpringBootTest
@Transactional
class IsolatedTest {
    // 각 테스트마다 롤백
}
```

### 4. 적절한 타임아웃 설정

```java
@Test
@Timeout(value = 5, unit = TimeUnit.SECONDS) // 5초 초과 시 실패
void longRunningTest() {
    // ...
}
```

### 5. Flaky Test 방지

```java
// ❌ 시간에 의존하는 테스트
@Test
void flakyTest() {
    service.asyncOperation();
    Thread.sleep(1000); // 불안정
    assertThat(result).isNotNull();
}

// ✅ Awaitility 사용
@Test
void stableTest() {
    service.asyncOperation();
    await().atMost(5, SECONDS)
            .until(() -> result != null);
}
```

## 통합 테스트 최적화

### 1. 테스트 실행 시간 단축

```java
// 컨테이너 재사용
@Testcontainers
class FastTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withReuse(true); // 재사용 활성화
}
```

### 2. 병렬 실행

```properties
# junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
```

```java
@Execution(ExecutionMode.CONCURRENT) // 클래스 레벨
class ParallelTest {

    @Test
    @Execution(ExecutionMode.CONCURRENT) // 메서드 레벨
    void test1() { }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void test2() { }
}
```

### 3. 선택적 실행

```java
@Tag("slow")
@Test
void slowIntegrationTest() { }

@Tag("fast")
@Test
void fastTest() { }
```

```bash
# fast 태그만 실행
./gradlew test --tests "*" -Dgroups="fast"
```

## 체크리스트

통합 테스트 작성 전:
- [ ] 이 기능은 정말 통합 테스트가 필요한가? (단위 테스트로 충분하지 않은가?)
- [ ] 어떤 컴포넌트들의 통합을 검증하는가?
- [ ] 실제 외부 시스템이 필요한가? (DB, Redis, Kafka 등)

통합 테스트 작성 후:
- [ ] 테스트가 독립적으로 실행되는가?
- [ ] 테스트 실행 시간이 합리적인가? (수 초 이내)
- [ ] Testcontainers를 효율적으로 재사용하고 있는가?
- [ ] 동시성 문제를 고려했는가? (필요한 경우)
- [ ] 테스트가 실패했을 때 원인을 파악하기 쉬운가?

## 관련 문서

- [단위 테스트 (Unit Testing)](./unit-testing.md)
- [E2E 테스트 (E2E Testing)](./e2e-testing.md)
- [동시성 제어 패턴 (TBD)](../../patterns/concurrency-patterns.md)
- [트랜잭션 관리 (TBD)](../../patterns/transaction-management.md)

## 참고 자료

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Awaitility Documentation](https://github.com/awaitility/awaitility)
- [Testing Microservices](https://martinfowler.com/articles/microservice-testing/)
- [Database Testing Best Practices](https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/)
