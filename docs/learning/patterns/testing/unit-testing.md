# 단위 테스트 (Unit Testing)

## 학습 목표

- JUnit 5를 사용한 Java 단위 테스트 작성법 이해
- Mockito를 활용한 의존성 격리 기법 습득
- 테스트 가능한 코드 설계 원칙 학습
- Given-When-Then 패턴을 통한 명확한 테스트 구조화
- AssertJ를 사용한 유창하고 가독성 높은 검증 작성

## 테스트 피라미드에서의 위치

```
        /\
       /  \      E2E Tests (소수, 느림, 비싸다)
      /____\
     /      \    Integration Tests (중간, 적당한 속도)
    /________\
   /          \  Unit Tests (다수, 빠름, 저렴하다) ← 여기!
  /__________\
```

**단위 테스트의 특징:**
- 가장 많은 수의 테스트가 이 계층에 위치해야 함
- 실행 속도가 빠르고 피드백이 즉각적
- 외부 의존성 없이 순수한 로직만 검증
- 낮은 유지보수 비용

## 단위 테스트란?

단위 테스트는 소프트웨어의 가장 작은 단위(함수, 메서드, 클래스)를 독립적으로 테스트하는 것입니다.

**핵심 원칙:**
- **Fast**: 수천 개의 테스트가 몇 초 안에 실행되어야 함
- **Independent**: 테스트 간 의존성이 없어야 함
- **Repeatable**: 언제 어디서든 같은 결과
- **Self-Validating**: 성공/실패가 명확해야 함
- **Timely**: 프로덕션 코드와 함께 작성

## JUnit 5 기본 구조

### 1. 기본 어노테이션

```java
import org.junit.jupiter.api.*;

class CalculatorTest {

    @BeforeAll
    static void initAll() {
        // 클래스 시작 전 한 번만 실행 (static 필수)
    }

    @BeforeEach
    void init() {
        // 각 테스트 메서드 실행 전마다 실행
    }

    @Test
    @DisplayName("두 숫자를 더하면 합계를 반환한다")
    void add() {
        Calculator calc = new Calculator();
        int result = calc.add(2, 3);
        assertEquals(5, result);
    }

    @AfterEach
    void tearDown() {
        // 각 테스트 메서드 실행 후마다 실행
    }

    @AfterAll
    static void tearDownAll() {
        // 모든 테스트 완료 후 한 번만 실행
    }
}
```

### 2. @Nested를 사용한 테스트 그룹화

```java
@DisplayName("장바구니 테스트")
class CartTest {

    @Nested
    @DisplayName("상품 추가 테스트")
    class AddItemTest {

        @Test
        @DisplayName("상품을 추가할 수 있다")
        void addItem() {
            // 관련된 테스트들을 논리적으로 그룹화
        }

        @Test
        @DisplayName("중복 상품 추가 시 예외 발생")
        void addDuplicateItem() {
            // ...
        }
    }

    @Nested
    @DisplayName("상품 제거 테스트")
    class RemoveItemTest {
        // ...
    }
}
```

**@Nested의 장점:**
- 테스트를 의미있는 그룹으로 구조화
- 테스트 리포트 가독성 향상
- 각 그룹별 setup/teardown 가능

## Given-When-Then 패턴

테스트 코드를 3단계로 명확하게 구조화하는 패턴입니다.

```java
@Test
@DisplayName("장바구니에 상품을 추가하면 총액이 증가한다")
void addItemIncreasesTotalAmount() {
    // Given - 테스트 준비 (데이터, 객체 생성)
    Cart cart = Cart.builder()
            .userId("user-123")
            .build();
    BigDecimal price = new BigDecimal("10000");

    // When - 테스트 실행 (실제 테스트할 동작)
    cart.addItem(1L, "Product", price, 2);

    // Then - 검증 (기대하는 결과 확인)
    assertThat(cart.getTotalAmount())
            .isEqualByComparingTo(new BigDecimal("20000"));
}
```

**주석이 없어도 명확한 구조:**
```java
@Test
void addItemIncreasesTotalAmount() {
    Cart cart = createActiveCart();
    cart.addItem(1L, "Product", new BigDecimal("10000"), 2);

    assertThat(cart.getTotalAmount())
            .isEqualByComparingTo(new BigDecimal("20000"));
}
```

## AssertJ - 유창한 Assertion

AssertJ는 가독성 높은 assertion을 제공하는 라이브러리입니다.

### 기본 검증

```java
import static org.assertj.core.api.Assertions.*;

@Test
void assertJExamples() {
    // 값 비교
    assertThat(actual).isEqualTo(expected);
    assertThat(text).isNotNull();
    assertThat(age).isGreaterThan(18);

    // 문자열
    assertThat(name).startsWith("John");
    assertThat(email).contains("@");
    assertThat(message).isBlank();

    // 숫자
    assertThat(price).isEqualByComparingTo(new BigDecimal("10000"));
    assertThat(count).isBetween(1, 10);

    // 컬렉션
    assertThat(list).hasSize(3);
    assertThat(list).isEmpty();
    assertThat(list).contains(item1, item2);
    assertThat(list).containsExactly(item1, item2, item3);

    // Optional
    assertThat(optional).isPresent();
    assertThat(optional).isEmpty();
    assertThat(optional).contains(expectedValue);
}
```

### 예외 검증

```java
@Test
@DisplayName("빈 장바구니 체크아웃 시 예외 발생")
void checkoutEmptyCartThrowsException() {
    Cart cart = createActiveCart();

    assertThatThrownBy(() -> cart.checkout())
            .isInstanceOf(CustomBusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ShoppingErrorCode.CART_EMPTY);
}
```

### 객체 필드 검증

```java
@Test
void verifyObjectFields() {
    User user = userService.create("john@example.com");

    assertThat(user)
            .extracting("email", "active", "role")
            .containsExactly("john@example.com", true, "USER");
}
```

## Portal Universe 테스트 코드 분석

### 예제 1: 도메인 엔티티 테스트 (CartTest)

**위치:** `services/shopping-service/src/test/java/.../cart/domain/CartTest.java`

```java
@Nested
@DisplayName("장바구니 항목 추가 테스트")
class AddItemTest {

    @Test
    @DisplayName("여러 상품을 추가하면 총액이 합산된다")
    void addMultipleItemsCalculatesTotalAmount() {
        // given
        Cart cart = createActiveCart();
        cart.addItem(1L, "Product 1", new BigDecimal("10000"), 2); // 20000
        cart.addItem(2L, "Product 2", new BigDecimal("5000"), 3);  // 15000

        // when
        BigDecimal totalAmount = cart.getTotalAmount();

        // then
        assertThat(totalAmount).isEqualByComparingTo(new BigDecimal("35000"));
        assertThat(cart.getTotalQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("이미 있는 상품을 추가하면 예외가 발생한다")
    void addDuplicateItemThrowsException() {
        // given
        Cart cart = createActiveCart();
        cart.addItem(1L, "Product", new BigDecimal("10000"), 1);

        // when & then
        assertThatThrownBy(() ->
                cart.addItem(1L, "Same Product", new BigDecimal("10000"), 1))
                .isInstanceOf(CustomBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ShoppingErrorCode.CART_ITEM_ALREADY_EXISTS);
    }
}

private Cart createActiveCart() {
    return Cart.builder()
            .userId("test-user")
            .build();
}
```

**학습 포인트:**
- `@Nested`로 관련 테스트 그룹화
- `@DisplayName`으로 테스트 의도 명확하게 표현
- 도메인 로직을 외부 의존성 없이 순수하게 테스트
- Helper 메서드(`createActiveCart`)로 테스트 코드 재사용
- 예외 발생 시나리오도 명시적으로 테스트

### 예제 2: 외부 시스템 Mock 테스트 (MockPGClientTest)

**위치:** `services/shopping-service/src/test/java/.../payment/pg/MockPGClientTest.java`

```java
class MockPGClientTest {

    private MockPGClient mockPGClient;

    @BeforeEach
    void setUp() {
        mockPGClient = new MockPGClient();
    }

    @Test
    @DisplayName("성공한 결제를 취소할 수 있다")
    void cancelSuccessfulPayment() {
        // given - 먼저 성공적인 결제를 수행
        String paymentNumber = "PAY-TEST0002";
        BigDecimal amount = new BigDecimal("50000");
        PaymentMethod method = PaymentMethod.CARD;

        // 결제가 성공할 때까지 재시도 (90% 성공률이므로 몇 번 안에 성공함)
        PgResponse paymentResponse = null;
        for (int i = 0; i < 20; i++) {
            paymentResponse = mockPGClient.processPayment(
                    paymentNumber + i, amount, method, "****");
            if (paymentResponse.success()) {
                break;
            }
        }

        assertThat(paymentResponse.success()).isTrue();
        String transactionId = paymentResponse.transactionId();

        // when
        PgResponse cancelResponse = mockPGClient.cancelPayment(transactionId);

        // then
        assertThat(cancelResponse.success()).isTrue();
    }
}
```

**학습 포인트:**
- `@BeforeEach`로 각 테스트마다 새로운 객체 생성
- 외부 PG사 API를 Mock으로 대체하여 순수 단위 테스트
- 랜덤 성공률을 가진 시스템에 대한 테스트 전략
- 복합 시나리오(결제 → 취소) 테스트

## Mockito를 사용한 의존성 격리

실제 객체 대신 Mock 객체를 사용하여 의존성을 격리합니다.

### 기본 사용법

```java
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder() {
        // given
        Order order = new Order();
        when(inventoryService.reserveStock(anyLong(), anyInt()))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        // when
        Order result = orderService.create(order);

        // then
        assertThat(result).isNotNull();
        verify(inventoryService).reserveStock(anyLong(), anyInt());
        verify(orderRepository).save(any(Order.class));
    }
}
```

### Stubbing (행동 정의)

```java
// 특정 입력에 대한 반환값 정의
when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

// 예외 던지기
when(paymentService.process(any()))
        .thenThrow(new PaymentException("Insufficient balance"));

// 여러 번 호출 시 다른 값 반환
when(random.nextInt())
        .thenReturn(1, 2, 3);

// void 메서드에 예외 던지기
doThrow(new RuntimeException())
        .when(emailService).send(anyString());
```

### Verification (호출 검증)

```java
// 메서드가 호출되었는지 확인
verify(userRepository).save(any(User.class));

// 정확히 n번 호출되었는지 확인
verify(notificationService, times(3)).send(anyString());

// 한 번도 호출되지 않았는지 확인
verify(paymentService, never()).process(any());

// 호출 순서 확인
InOrder inOrder = inOrder(inventoryService, orderService);
inOrder.verify(inventoryService).reserve(anyLong());
inOrder.verify(orderService).create(any());
```

## 테스트 가능한 코드 설계

### 1. 생성자 주입 사용

```java
// ❌ 나쁜 예: Field Injection
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository; // 테스트 시 주입 어려움
}

// ✅ 좋은 예: Constructor Injection
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository; // final로 불변성 보장
}
```

### 2. 순수 함수 지향

```java
// ❌ 테스트하기 어려운 코드
public class PriceCalculator {
    public BigDecimal calculate(Order order) {
        BigDecimal now = new BigDecimal(System.currentTimeMillis()); // 시간 의존
        return order.getAmount().multiply(now); // 예측 불가능
    }
}

// ✅ 테스트하기 쉬운 코드
public class PriceCalculator {
    public BigDecimal calculate(Order order, LocalDateTime timestamp) {
        return order.getAmount().multiply(getMultiplier(timestamp));
    }
}
```

### 3. 도메인 로직을 Entity로 이동

```java
// ❌ Service에 비즈니스 로직 집중
public class CartService {
    public BigDecimal getTotalAmount(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// ✅ Entity에 도메인 로직 캡슐화 (Repository 없이 테스트 가능)
@Entity
public class Cart {
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## 모범 사례

### 1. 테스트 이름 명명 규칙

```java
// ✅ 한글로 명확하게
@DisplayName("가용 재고보다 많은 수량을 예약하면 예외가 발생한다")
void reserveExceedingStockThrowsException() { }

// ✅ 영어로 명확하게 (when_should 패턴)
@Test
void whenReservingMoreThanAvailable_shouldThrowException() { }

// ❌ 불명확한 이름
@Test
void test1() { }
```

### 2. 하나의 테스트는 하나의 개념만

```java
// ❌ 여러 개념을 한 테스트에서 검증
@Test
void testCart() {
    cart.addItem(...);
    assertThat(cart.getItemCount()).isEqualTo(1);

    cart.checkout();
    assertThat(cart.getStatus()).isEqualTo(CHECKED_OUT);

    cart.clear();
    assertThat(cart.getItems()).isEmpty();
}

// ✅ 개념별로 분리
@Test
void addItem_increasesItemCount() { }

@Test
void checkout_changesStatus() { }

@Test
void clear_removesAllItems() { }
```

### 3. 테스트 데이터 빌더 패턴

```java
// Helper 클래스로 테스트 데이터 생성 간소화
public class TestDataBuilder {

    public static Cart createActiveCart(String userId) {
        return Cart.builder()
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .build();
    }

    public static CartItem createCartItem(Long productId, int quantity) {
        return CartItem.builder()
                .productId(productId)
                .productName("Test Product " + productId)
                .price(new BigDecimal("10000"))
                .quantity(quantity)
                .build();
    }
}
```

### 4. 테스트 격리

```java
// ✅ 각 테스트는 독립적
@Test
void test1() {
    Cart cart = new Cart(); // 새로운 객체
    // ...
}

@Test
void test2() {
    Cart cart = new Cart(); // 다른 독립적인 객체
    // ...
}

// ❌ 테스트 간 상태 공유 (순서 의존성 발생)
private Cart sharedCart = new Cart();

@Test
void test1() {
    sharedCart.addItem(...);
}

@Test
void test2() {
    // test1의 실행 결과에 의존
}
```

## 실전 팁

### 1. 경계값 테스트

```java
@Test
void boundaryTests() {
    // 최소값
    assertThat(calculateDiscount(0)).isEqualTo(0);

    // 최대값
    assertThat(calculateDiscount(100)).isEqualTo(10);

    // 경계값
    assertThat(calculateDiscount(1)).isGreaterThan(0);
    assertThat(calculateDiscount(99)).isLessThan(10);

    // 예외 케이스
    assertThatThrownBy(() -> calculateDiscount(-1))
            .isInstanceOf(IllegalArgumentException.class);
}
```

### 2. @ParameterizedTest로 반복 제거

```java
@ParameterizedTest
@CsvSource({
    "0, 0",
    "10, 1",
    "50, 5",
    "100, 10"
})
void calculateDiscountForVariousAmounts(int amount, int expectedDiscount) {
    assertThat(calculateDiscount(amount)).isEqualTo(expectedDiscount);
}
```

### 3. Custom Assertions

```java
public class CartAssertions {

    public static CartAssert assertThat(Cart cart) {
        return new CartAssert(cart);
    }

    public static class CartAssert extends AbstractAssert<CartAssert, Cart> {

        public CartAssert(Cart cart) {
            super(cart, CartAssert.class);
        }

        public CartAssert hasItemCount(int expected) {
            assertThat(actual.getItemCount()).isEqualTo(expected);
            return this;
        }

        public CartAssert hasStatus(CartStatus expected) {
            assertThat(actual.getStatus()).isEqualTo(expected);
            return this;
        }
    }
}

// 사용
@Test
void testCart() {
    Cart cart = createActiveCart();
    cart.addItem(...);

    assertThat(cart)
            .hasItemCount(1)
            .hasStatus(CartStatus.ACTIVE);
}
```

## 체크리스트

테스트 작성 전:
- [ ] 이 테스트는 정말 "단위" 테스트인가? (외부 의존성 없는가?)
- [ ] 테스트하려는 단 하나의 개념이 명확한가?
- [ ] Given-When-Then 구조로 작성할 수 있는가?

테스트 작성 후:
- [ ] 테스트 이름만 봐도 무엇을 검증하는지 알 수 있는가?
- [ ] 테스트가 실패했을 때 원인을 바로 파악할 수 있는가?
- [ ] 다른 테스트의 실행 순서나 결과에 영향을 받지 않는가?
- [ ] 1초 이내에 실행되는가?

## 관련 문서

- [통합 테스트 (Integration Testing)](./integration-testing.md)
- [E2E 테스트 (E2E Testing)](./e2e-testing.md)
- [Clean Code - 함수](../functions.md)
- [Testing Patterns (TBD)](../../patterns/testing-patterns.md)

## 참고 자료

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Test Driven Development by Kent Beck](https://www.amazon.com/Test-Driven-Development-Kent-Beck/dp/0321146530)
- [Growing Object-Oriented Software, Guided by Tests](https://www.amazon.com/Growing-Object-Oriented-Software-Guided-Tests/dp/0321503627)
