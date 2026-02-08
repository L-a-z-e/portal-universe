# Common Library API Documentation

> Portal Universe 마이크로서비스 공통 라이브러리 API 명세서입니다.

---

## 개요

| 항목 | 내용 |
|------|------|
| **라이브러리명** | common-library |
| **버전** | 0.0.1-SNAPSHOT |
| **Java 버전** | 17 |
| **Spring Boot** | 3.5.5 |
| **패키지** | `com.portal.universe.commonlibrary` |

---

## API 문서 목록

| 문서 | 설명 | 상태 |
|------|------|------|
| [core-api.md](./core-api.md) | 표준 응답 포맷(ApiResponse, ErrorResponse), 예외 처리(ErrorCode, GlobalExceptionHandler) | current |
| [security-auth.md](./security-auth.md) | JWT 인증, Gateway 필터, 사용자 컨텍스트, AuthConstants | current |
| [security-validation.md](./security-validation.md) | XSS/SQL Injection 방어 어노테이션(@NoXss, @SafeHtml, @NoSqlInjection), 유틸리티 | current |
| [security-audit.md](./security-audit.md) | 보안 감사 로그(@AuditLog, SecurityAuditEvent, SecurityAuditService) | current |

---

## 패키지 구조

```
com.portal.universe.commonlibrary/
├── response/                  # 표준 응답 포맷
│   ├── ApiResponse<T>
│   └── ErrorResponse
├── exception/                 # 예외 처리
│   ├── ErrorCode (Interface)
│   ├── CommonErrorCode
│   ├── CustomBusinessException
│   └── GlobalExceptionHandler
├── security/
│   ├── config/                # 자동 설정
│   │   ├── JwtSecurityAutoConfiguration
│   │   └── AuthUserWebConfig
│   ├── converter/             # JWT 변환기
│   │   ├── JwtAuthenticationConverterAdapter
│   │   └── ReactiveJwtAuthenticationConverterAdapter
│   ├── filter/                # 보안 필터
│   │   ├── GatewayAuthenticationFilter
│   │   └── XssFilter
│   ├── context/               # 사용자 컨텍스트
│   │   ├── @CurrentUser
│   │   ├── CurrentUserArgumentResolver
│   │   ├── AuthUser (Record)
│   │   ├── SecurityUtils
│   │   └── MembershipContext
│   ├── constants/             # 인증 상수
│   │   └── AuthConstants
│   ├── xss/                   # XSS 방어
│   │   ├── @NoXss
│   │   ├── NoXssValidator
│   │   ├── @SafeHtml
│   │   ├── SafeHtmlValidator
│   │   └── XssUtils
│   ├── sql/                   # SQL Injection 방어
│   │   ├── @NoSqlInjection
│   │   ├── NoSqlInjectionValidator
│   │   └── SqlInjectionUtils
│   └── audit/                 # 감사 로그
│       ├── @AuditLog
│       ├── AuditLogAspect
│       ├── SecurityAuditEvent
│       ├── SecurityAuditEventType
│       ├── SecurityAuditService (Interface)
│       └── SecurityAuditServiceImpl
└── util/                      # 유틸리티
    └── IpUtils
```

---

## 관련 문서

- [Common Library 사용 가이드](../../guides/development/common-library-usage.md)
- [Auth Service API](../auth-service/README.md)

---

**최종 업데이트**: 2026-02-08
