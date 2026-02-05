# Common Library API 명세서

Portal Universe 마이크로서비스 공통 라이브러리 API 문서 목록입니다.

---

## API 문서 목록

| ID | 제목 | 범위 | 버전 | 상태 | 최종 수정 |
|----|------|------|------|------|----------|
| [API-001](./API-001-common-library.md) | Core - 응답/예외 | ApiResponse, ErrorResponse, ErrorCode, GlobalExceptionHandler | v2 | current | 2026-02-06 |
| [API-002](./API-002-security-auth.md) | Security - 인증 | JWT, Gateway 필터, 사용자 컨텍스트, AuthConstants | v1 | current | 2026-02-06 |
| [API-003](./API-003-security-validation.md) | Security - 입력 검증 | @NoXss, @SafeHtml, @NoSqlInjection, XssUtils, SqlInjectionUtils, IpUtils | v1 | current | 2026-02-06 |
| [API-004](./API-004-security-audit.md) | Security - 감사 로그 | @AuditLog, SecurityAuditEvent, SecurityAuditService | v1 | current | 2026-02-06 |

---

## 패키지 구조

```
com.portal.universe.commonlibrary/
├── response/                  # API-001: 표준 응답 포맷
│   ├── ApiResponse<T>
│   └── ErrorResponse
├── exception/                 # API-001: 예외 처리
│   ├── ErrorCode (Interface)
│   ├── CommonErrorCode
│   ├── CustomBusinessException
│   └── GlobalExceptionHandler
├── security/
│   ├── config/                # API-002: 자동 설정
│   │   ├── JwtSecurityAutoConfiguration
│   │   └── GatewayUserWebConfig
│   ├── converter/             # API-002: JWT 변환기
│   │   ├── JwtAuthenticationConverterAdapter
│   │   └── ReactiveJwtAuthenticationConverterAdapter
│   ├── filter/                # API-002, API-003: 필터
│   │   ├── GatewayAuthenticationFilter
│   │   └── XssFilter
│   ├── context/               # API-002: 사용자 컨텍스트
│   │   ├── @CurrentUser
│   │   ├── CurrentUserArgumentResolver
│   │   ├── GatewayUser (Record)
│   │   ├── SecurityUtils
│   │   └── MembershipContext
│   ├── constants/             # API-002: 인증 상수
│   │   └── AuthConstants
│   ├── xss/                   # API-003: XSS 방어
│   │   ├── @NoXss
│   │   ├── NoXssValidator
│   │   ├── @SafeHtml
│   │   ├── SafeHtmlValidator
│   │   └── XssUtils
│   ├── sql/                   # API-003: SQL Injection 방어
│   │   ├── @NoSqlInjection
│   │   ├── NoSqlInjectionValidator
│   │   └── SqlInjectionUtils
│   └── audit/                 # API-004: 감사 로그
│       ├── @AuditLog
│       ├── AuditLogAspect
│       ├── SecurityAuditEvent
│       ├── SecurityAuditEventType
│       ├── SecurityAuditService (Interface)
│       └── SecurityAuditServiceImpl
└── util/                      # API-003: 유틸리티
    └── IpUtils
```

---

## 문서 규칙

- **명명 규칙**: `API-XXX-[domain].md`
- **ID 부여**: 순차 번호 (001, 002, ...)
- **상태**:
  - current: 현재 사용 중
  - draft: 작성 중
  - deprecated: 더 이상 사용하지 않음

---

**최종 업데이트**: 2026-02-06
