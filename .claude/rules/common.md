# Common Rules

언어/프레임워크 무관 공통 규칙입니다.

## Commit Convention

### Format
```
<type>(<scope>): <subject>

[optional body]
[optional footer]
```

### Types
| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Code style (formatting, no logic change) |
| `refactor` | Code refactoring |
| `test` | Adding/updating tests |
| `chore` | Build, config, dependencies |

### Scope Examples
- `auth`, `blog`, `shopping`, `notification` - Backend services
- `shell`, `blog-fe`, `shopping-fe`, `design-system` - Frontend modules
- `gateway`, `config` - Infrastructure services
- `k8s`, `docker`, `ci` - DevOps

### Examples
```
feat(auth): add password reset API endpoint
fix(blog-fe): resolve infinite scroll bug on mobile
refactor(shopping): extract price calculation to utility
```

---

## Error Handling

### Pattern
ErrorCode Interface → Enum → CustomBusinessException → GlobalExceptionHandler

### Service Error Code Prefixes

| Service | Prefix | Example |
|---------|--------|---------|
| Common | C | C001, C002, C003 |
| Auth | A | A001 |
| Blog | B | B001, B002, B003 |
| Shopping | S | S001 (S0XX~S9XX 도메인별, S10XX 검색) |
| Notification | N | N001 |

### Core Files Location
```
services/common-library/.../exception/
├── ErrorCode.java              # ErrorCode interface
├── CommonErrorCode.java        # Common error codes enum
├── CustomBusinessException.java # Custom business exception
└── GlobalExceptionHandler.java  # Global exception handler

services/common-library/.../response/
└── ApiResponse.java            # Unified response wrapper
```

---

## Token Optimization

### Do NOT
- Read the same file multiple times in one session
- Scan entire directories when specific file is known
- Regenerate known information from previous conversation

### DO
- Reuse information from earlier in conversation
- Use targeted file reads instead of directory scans
- Reference `.claude/rules/` files instead of repeating patterns

### Search Strategy
1. Start with specific file if path is known
2. Use glob patterns for targeted searches
3. Only explore broadly when necessary

### Code Generation
- Generate minimal viable code
- Avoid unnecessary abstractions
- Don't add features not requested

---

## API Response

모든 API 응답은 `ApiResponse` wrapper 사용:

```java
// 성공
return ResponseEntity.ok(ApiResponse.success(data));

// 실패 (CustomBusinessException 발생 시 자동 처리)
throw new CustomBusinessException(ErrorCode.NOT_FOUND);
```

### 응답 구조
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

---

## Logging

| Level | 용도 | 예시 |
|-------|------|------|
| `INFO` | 비즈니스 이벤트 | 주문 생성, 결제 완료 |
| `DEBUG` | 상세 흐름 | 메소드 진입/종료, 변수 값 |
| `WARN` | 잠재적 문제 | 재시도, 폴백 |
| `ERROR` | 예외 + 스택트레이스 | 처리 불가 오류 |

```java
log.info("Order created: orderId={}, userId={}", orderId, userId);
log.error("Payment failed: orderId={}", orderId, exception);
```
