---
id: test-strategy
title: Portal Universe 테스트 전략
type: test-strategy
status: current
created: 2026-01-19
updated: 2026-01-19
author: QA Team
---

# 테스트 전략

## 개요

Portal Universe의 품질 보증을 위한 테스트 전략입니다.

## 테스트 피라미드

```
          /\
         /  \
        / E2E \
       /______\
      /        \
     /Integration\
    /______________\
   /                \
  /     Unit Tests   \
 /____________________\
```

| 레벨 | 비율 | 설명 |
|------|------|------|
| Unit | 70% | 개별 함수/클래스 검증 |
| Integration | 20% | 서비스 간 통합 검증 |
| E2E | 10% | 전체 시나리오 검증 |

## Backend 테스트

### Unit Test

**도구**: JUnit 5, Mockito

**대상**:
- Service Layer 비즈니스 로직
- Utility 클래스
- Domain 객체 검증

**예시**:
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_WithValidRequest_ShouldCreateOrder() {
        // given
        OrderRequest request = new OrderRequest(...);

        // when
        OrderResponse response = orderService.createOrder(request);

        // then
        assertThat(response).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }
}
```

### Integration Test

**도구**: Spring Boot Test, Testcontainers

**대상**:
- Repository Layer (DB 연동)
- Controller Layer (API 엔드포인트)
- 외부 서비스 연동 (Feign Client)

**예시**:
```java
@SpringBootTest
@Testcontainers
class ProductRepositoryIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findById_WithExistingProduct_ShouldReturnProduct() {
        // ...
    }
}
```

### 테스트 커버리지 목표

| 패키지 | 목표 | 현재 |
|--------|------|------|
| service | 80% | TBD |
| controller | 70% | TBD |
| repository | 60% | TBD |

## Frontend 테스트

### Unit Test

**도구**: Vitest, Testing Library

**대상**:
- React/Vue 컴포넌트
- Custom Hooks
- Utility 함수
- Store 로직

**예시**:
```typescript
import { render, screen } from '@testing-library/react';
import { ProductCard } from './ProductCard';

describe('ProductCard', () => {
  it('renders product name', () => {
    render(<ProductCard name="Test Product" price={10000} />);
    expect(screen.getByText('Test Product')).toBeInTheDocument();
  });
});
```

### E2E Test

**도구**: Playwright

**대상**:
- 핵심 사용자 시나리오
- 크로스 브라우저 호환성
- Module Federation 통합

**예시**:
```typescript
import { test, expect } from '@playwright/test';

test('user can complete checkout flow', async ({ page }) => {
  await page.goto('/shopping/products');
  await page.click('[data-testid="add-to-cart"]');
  await page.click('[data-testid="checkout"]');
  await expect(page.locator('.order-confirmation')).toBeVisible();
});
```

## 테스트 환경

| 환경 | 용도 | DB |
|------|------|-----|
| Local | 개발 중 테스트 | H2 (embedded) |
| CI | PR 검증 | Testcontainers |
| Staging | QA 테스트 | MySQL (dedicated) |

## CI/CD 통합

### PR 체크

1. Unit Test 실행
2. Integration Test 실행
3. Coverage Report 생성
4. Lint/Format 검사

### Pre-Production

1. 전체 테스트 실행
2. E2E Test (Staging)
3. 성능 테스트 (선택)

## 테스트 데이터 관리

### Backend
- `@Sql` 어노테이션으로 테스트 데이터 주입
- `@DirtiesContext`로 컨텍스트 격리

### Frontend
- MSW (Mock Service Worker)로 API Mocking
- Faker.js로 테스트 데이터 생성

## 관련 문서

- [Testing 문서 작성 가이드](../../docs_template/guide/testing/how-to-write.md)
- [TP-001-01 Shopping API 테스트 계획](./test-plan/TP-001-01-shopping-api.md)
