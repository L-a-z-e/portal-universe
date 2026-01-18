# ADR-003: Admin 권한 검증 전략

## 상태
**Accepted**

## 날짜
2026-01-17

---

## 컨텍스트

Admin 기능 구현 시 권한 검증이 필수적입니다. 다음 시나리오를 고려해야 합니다:

1. Admin이 아닌 사용자가 `/admin` 페이지에 접근 시도
2. Admin이 권한 없는 API를 호출하는 경우 (API 직접 호출)
3. 네트워크 오류로 권한 검증이 실패하는 경우
4. 권한이 변경되어 기존 사용자의 권한이 박탈되는 경우

### 보안 원칙

권한 검증은 **심층 방어(Defense in Depth)** 원칙을 따라야 합니다:

```
Frontend (UX 보호) → API Gateway (인증) → Backend (인가)
```

---

## 결정

**Frontend Route Guard + Backend @PreAuthorize를 조합한 심층 방어 전략을 채택합니다.**

### 계층별 구현

#### 1. Frontend Route Guard (UX 보호)

**목적**: 권한 없는 사용자가 Admin 페이지에 접근하는 것을 미리 방지

```typescript
// src/components/guards/RequireRole.tsx
export const RequireRole: React.FC<{
  children: React.ReactNode;
  roles: string[];
}> = ({ children, roles }) => {
  const { user } = useAuthStore();

  const hasRole = user?.roles?.some(role => roles.includes(role));

  if (!hasRole) {
    return <Navigate to="/403" replace />;
  }

  return <>{children}</>;
};

// src/router/index.tsx - 라우트 적용
{
  path: 'admin/products',
  element: (
    <RequireRole roles={['ROLE_ADMIN']}>
      <AdminProductListPage />
    </RequireRole>
  )
}
```

**특징**:
- Frontend에서만 동작 (JavaScript 제어)
- 즉각적인 UX 피드백
- API 불필요한 호출 방지

**주의**: Frontend 권한 검증은 우회 가능하므로 보안을 보장하지 않음

#### 2. API Gateway 인증 검증

**목적**: 토큰 유효성 검증 (인증)

```java
// API Gateway의 SecurityConfig
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(authenticationConverter())
                )
            );
        return http.build();
    }
}
```

**역할**:
- JWT 토큰 검증 (서명, 만료 시간, Issuer)
- 인증 실패 시 401 Unauthorized 반환
- 모든 백엔드 서비스 요청 전에 검증

#### 3. Shopping Service 권한 검증 (인가)

**목적**: 실제 권한 검증 (인가) - 가장 중요한 계층

```java
// ProductController.java
@RestController
@RequestMapping("/api/shopping/product")
public class ProductController {

    // 공개 API (인증 불필요)
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    // Admin 전용 API (권한 검증)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.createProduct(request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
    }
}
```

**특징**:
- Method Level Security로 각 메서드별 권한 정의
- JWT에서 추출한 역할(role) 검사
- 권한 없음 시 403 Forbidden 반환

#### 4. Business Logic 수준 검증

**목적**: Resource Owner 검증 (본인 확인)

```java
// OrderService.java
@Service
public class OrderService {

    /**
     * 주문 조회 - 본인 주문만 허용
     */
    public OrderResponse getOrder(Long orderId, String currentUserEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CustomBusinessException(
                ShoppingErrorCode.ORDER_NOT_FOUND
            ));

        // 본인 주문인지 확인 (Resource Owner 검증)
        if (!order.getUserEmail().equals(currentUserEmail)) {
            throw new CustomBusinessException(
                ShoppingErrorCode.UNAUTHORIZED_ORDER_ACCESS
            );
        }

        return OrderResponse.from(order);
    }
}
```

---

## 대안 검토

| 대안 | 장점 | 단점 | 평가 |
|------|------|------|------|
| **Frontend만 검증** | 빠른 구현 | 보안 보장 없음, API 직접 호출 시 우회 가능 | ❌ |
| **Backend만 검증** | 안전함 | UX 저하 (401/403 에러 발생 후 처리) | ⚠️ |
| **Frontend + Backend** | 안전성 + 좋은 UX | 약간 복잡함 | ✅ **선택** |
| **Middleware 중앙화** | 일관된 관리 | 복잡도 증가, 특수 케이스 처리 어려움 | ❌ |

### 선택 근거

1. **보안 보장**
   - Backend `@PreAuthorize`가 최종 방어선 역할
   - Frontend 우회 불가능

2. **우수한 UX**
   - Frontend Route Guard로 불필요한 페이지 이동 방지
   - 비권한 사용자에게 빠른 피드백 제공
   - 네트워크 왕복 최소화

3. **명확한 책임 분리**
   - Frontend: 사용자 경험 보호
   - Backend: 실제 보안 보장

4. **장애 대응**
   - Backend 권한 검증 실패해도 안전 (Frontend는 보조적)
   - API 직접 호출 시에도 Backend에서 차단

---

## 결과

### 긍정적 영향

1. **강력한 보안**
   - 다층 방어로 권한 우회 불가능
   - Frontend 변조 시에도 Backend 검증으로 보호
   - 401/403 에러 로깅으로 감사 추적 가능

2. **우수한 사용자 경험**
   - Admin이 아닌 사용자는 Admin 메뉴 보이지 않음
   - Admin 페이지 접근 시도 시 즉시 403 페이지 표시
   - 불필요한 API 호출 방지

3. **확장 가능성**
   - 세분화된 권한 추가 시 Frontend/Backend 동시 업데이트
   - 권한 모델 변경 용이

4. **감사 추적(Audit)**
   - 모든 권한 위반이 Backend 로그에 기록
   - 보안 감사 가능

### 부정적 영향

1. **구현 복잡도 증가**
   - Frontend Route Guard 구현 필요
   - Backend `@PreAuthorize` 추가
   - 둘 다 유지보수 필요

2. **권한 동기화 필요**
   - Frontend의 권한 정보와 Backend가 불일치할 수 있음
   - 권한 변경 시 둘 다 반영 필요

3. **성능 영향**
   - Frontend Route Guard 체크로 약간의 오버헤드
   - API 호출 시 Backend 권한 검증 대기

### 완화 방안

1. **권한 정보 중앙화**
   - JWT 토큰에 권한 정보 포함
   - Backend에서 토큰 발급 시 정확한 권한 반영

2. **UI 컴포넌트 자동화**
   - `RequireRole` 고차 컴포넌트로 패턴화
   - 개발자가 자동으로 따르도록 유도

3. **에러 처리 표준화**
   - 401/403 에러 응답 형식 통일
   - Frontend Axios Interceptor에서 자동 처리

---

## 에러 처리 흐름

```
┌─────────────────────────────────────────────────────┐
│                    Frontend                          │
│  1. RequireRole 컴포넌트 체크                         │
│  2. 권한 없으면 /403 리다이렉트                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              API Gateway (8080)                      │
│  3. JWT 토큰 검증                                    │
│  4. 만료/없음 → 401 Unauthorized                    │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          Shopping Service (8083)                     │
│  5. @PreAuthorize("hasRole('ADMIN')")               │
│  6. 권한 없음 → 403 Forbidden                       │
│  7. AccessDeniedException 발생                       │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          GlobalExceptionHandler                      │
│  8. 예외 변환 → ApiResponse<Object>                 │
│  {                                                   │
│    "success": false,                                 │
│    "code": "S403",                                   │
│    "message": "접근 권한이 없습니다"                │
│  }                                                   │
└───────────────────┬─────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│          Axios Interceptor (Frontend)                │
│  9. 403 응답 처리                                    │
│  10. 에러 토스트 표시                                │
└─────────────────────────────────────────────────────┘
```

---

## 구현 가이드

### 1. Frontend Route Guard 구현

```typescript
// src/components/guards/RequireRole.tsx
interface RequireRoleProps {
  children: React.ReactNode;
  roles: string[];
  redirectTo?: string;
}

export const RequireRole: React.FC<RequireRoleProps> = ({
  children,
  roles,
  redirectTo = '/403'
}) => {
  const { user } = useAuthStore();

  const hasRequiredRole = user?.roles?.some(role => roles.includes(role));

  if (!hasRequiredRole) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
};
```

### 2. Backend 권한 검증 추가

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // 기본 설정 사용 (SpEL 표현식 지원)
}

// 또는 SecurityConfig에 추가
@Configuration
public class SecurityConfig {
    @Bean
    public AuthorizationManager<RequestAuthorizationContext> requestAuthorizationManager(
        RoleHierarchyAuthoritiesMapper roleHierarchyAuthoritiesMapper) {

        // Method Security와 HTTP Security의 권한 모델 통일
        return new RequestMatcherDelegatingAuthorizationManager(
            // Admin 경로는 ADMIN 역할 필요
            new RequestMatcher("POST", "/api/shopping/product/**"),
            // ... 기타 규칙
        );
    }
}
```

### 3. 에러 응답 처리

```typescript
// src/hooks/useApiError.ts
export const useApiError = () => {
  const { toast } = useToast();

  const handleError = useCallback((error: ApiError) => {
    switch (error.code) {
      case 'S403':
      case 'S403-10': // ADMIN_ONLY
        toast({
          type: 'error',
          message: '관리자 권한이 필요합니다'
        });
        break;

      case 'S403-01': // UNAUTHORIZED_ORDER_ACCESS
        toast({
          type: 'error',
          message: '본인의 주문만 조회할 수 있습니다'
        });
        break;

      default:
        toast({
          type: 'error',
          message: error.message || '오류가 발생했습니다'
        });
    }
  }, [toast]);

  return { handleError };
};
```

---

## 테스트 전략

### 1. Backend 권한 검증 테스트

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Test
    @DisplayName("ADMIN 역할로 상품 생성 성공")
    @WithMockUser(roles = "ADMIN")
    void createProduct_AdminRole_Success() throws Exception {
        mockMvc.perform(post("/api/shopping/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":1000}"))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("USER 역할로 상품 생성 실패")
    @WithMockUser(roles = "USER")
    void createProduct_UserRole_Forbidden() throws Exception {
        mockMvc.perform(post("/api/shopping/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":1000}"))
            .andExpect(status().isForbidden());
    }
}
```

### 2. Frontend Route Guard 테스트

```typescript
// tests/components/RequireRole.test.tsx
describe('RequireRole', () => {
  it('권한 있으면 컨텐츠 표시', () => {
    const mockUser = { roles: ['ROLE_ADMIN'] };
    // useAuthStore mock 설정

    const { getByText } = render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <div>Admin Content</div>
      </RequireRole>
    );

    expect(getByText('Admin Content')).toBeInTheDocument();
  });

  it('권한 없으면 리다이렉트', () => {
    const mockUser = { roles: ['ROLE_USER'] };
    // useAuthStore mock 설정

    const { queryByText } = render(
      <RequireRole roles={['ROLE_ADMIN']}>
        <div>Admin Content</div>
      </RequireRole>
    );

    expect(queryByText('Admin Content')).not.toBeInTheDocument();
  });
});
```

### 3. E2E 테스트

```typescript
// e2e-tests/tests/admin-auth.spec.ts
test('일반 사용자는 Admin 페이지 접근 불가', async ({ page }) => {
  await page.goto('http://localhost:30000/login');
  // 일반 사용자로 로그인

  await page.goto('http://localhost:30000/admin/products');

  // /403 또는 /login으로 리다이렉트 확인
  expect(page.url()).toMatch(/\/(403|login)/);
});
```

---

## 보안 체크리스트

- [ ] JWT 토큰 서명 검증 (API Gateway)
- [ ] JWT 토큰 만료 시간 검증 (API Gateway)
- [ ] `@PreAuthorize` 어노테이션 적용 (Admin 메서드)
- [ ] Frontend Route Guard 구현 (RequireRole)
- [ ] 401/403 에러 로깅 추가
- [ ] 에러 메시지에 민감한 정보 노출 금지
- [ ] Resource Owner 검증 구현 (본인 데이터만 조회)
- [ ] API Rate Limiting 설정 (필수 아님, 선택)

---

## 참고 자료

- 참고 문서: `/Users/laze/Laze/Project/portal-universe/docs/architecture/admin-authorization-strategy.md`
- OWASP: Broken Access Control (A01:2021)
- Spring Security: Method Security 공식 문서
- JWT: https://tools.ietf.org/html/rfc7519

---

## 다음 단계

1. **Backend 개선** (1-2일)
   - ProductController에 `@PreAuthorize` 추가
   - RequestBody Validation 추가
   - GlobalExceptionHandler 권한 에러 처리

2. **Frontend 구현** (1-2일)
   - RequireRole 컴포넌트 구현
   - Admin 라우트에 적용
   - 403 페이지 구현

3. **테스트 작성** (1-2일)
   - Backend 권한 검증 테스트
   - Frontend Route Guard 테스트
   - E2E 권한 시나리오 테스트

4. **문서화 및 배포** (1일)
   - API 문서에 권한 정보 추가
   - 운영 가이드 작성

---

**문서 버전**: 1.0
**작성자**: Documenter Agent
**최종 업데이트**: 2026-01-17
