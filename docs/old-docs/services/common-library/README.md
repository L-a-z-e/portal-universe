---
id: DOC-INDEX-COMMON-LIBRARY
title: Common Library 문서 인덱스
type: index
status: current
created: 2026-01-18
updated: 2026-01-30
author: Laze
tags: [common-library, index, documentation]
---

# Common Library - Portal Universe 공유 라이브러리

## 개요

`common-library`는 Portal Universe의 모든 마이크로서비스에서 사용하는 공유 라이브러리입니다.

이 모듈은 **API 응답 표준화**, **예외 처리**, **JWT 보안**, **도메인 이벤트** 등 시스템 전반에서 반복되는 기능을 중앙화하여 관리합니다. 각 마이크로서비스는 이 라이브러리를 의존성으로 추가하여 일관된 구조와 패턴을 유지할 수 있습니다.

## 주요 기능

| 기능 | 설명 |
|------|------|
| **ApiResponse** | 모든 REST API의 응답을 일관된 형식으로 표준화 |
| **예외 처리** | ErrorCode 인터페이스 → Enum → CustomBusinessException → GlobalExceptionHandler |
| **JWT 보안** | Spring Security 자동 설정, Servlet/Reactive 이중 지원, RBAC |
| **도메인 이벤트** | Kafka 기반 비동기 통신을 위한 표준 이벤트 클래스 |

서비스별 에러코드 접두사: Common(`C`), Auth(`A`), Blog(`B`), Shopping(`S`)

---

## 문서 목록

### 📐 아키텍처 문서
| ID | 문서명 | 설명 |
|----|--------|------|
| ARCH-001 | [Common Library Overview](architecture/ARCH-001-common-library-overview.md) | 전체 아키텍처 설계 및 주요 결정 사항 |

### 📚 API 문서
| ID | 문서명 | 설명 |
|----|--------|------|
| API-001 | [Common Library API Reference](api/API-001-common-library.md) | 공개 API 및 클래스 상세 명세 |

### 📐 보안 감사 아키텍처
| 문서명 | 설명 |
|--------|------|
| [보안 감사 로깅 모듈](architecture/security-audit-module.md) | 보안 감사 로깅 모듈 아키텍처, 13가지 이벤트 유형, AOP 기반 @AuditLog |

### 📖 개발 가이드
| ID | 문서명 | 설명 |
|----|--------|------|
| GUIDE-001 | [Common Library Usage Guide](guides/GUIDE-001-common-library-usage.md) | 각 서비스별 사용 방법 및 예제 |
| - | [보안 감사 로그 설정](guides/security-audit-log-setup.md) | Logback 설정, 사용 방법, 모니터링 연동 |
| - | [보안 모듈 가이드](guides/security-module.md) | XSS/SQL Injection 방지, 보안 헤더 설정 |

---

## 빠른 시작

### 1. 의존성 추가

```gradle
dependencies {
    implementation 'com.portal.universe:common-library:0.0.1-SNAPSHOT'
}
```

### 2. API 응답 활용

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(productService.getProduct(id)));
}
```

### 3. 예외 처리

```java
throw new CustomBusinessException(ShoppingErrorCode.PRODUCT_NOT_FOUND);
// → GlobalExceptionHandler에 의해 ApiResponse.error()로 자동 변환
```

> 상세 설정 및 이벤트 발행/구독 등은 [GUIDE-001 사용 가이드](guides/GUIDE-001-common-library-usage.md)를 참조하세요.

---

## 빌드

```bash
./gradlew :services:common-library:build
```

---

## 프로젝트 구조

```
common-library/
├── src/main/java/com/portal/universe/
│   ├── commonlibrary/
│   │   ├── response/        # ApiResponse, ErrorResponse
│   │   ├── exception/       # ErrorCode, CustomBusinessException, GlobalExceptionHandler
│   │   └── security/        # JWT 자동 설정 (Servlet + Reactive)
│   └── common/event/        # 도메인 이벤트 (UserSignedUp, Order, Payment 등)
└── build.gradle
```

---

## 관련 문서

- [Auth Service 문서](../../auth-service/docs/README.md)
- [Blog Service 문서](../../blog-service/docs/README.md)
- [Shopping Service 문서](../../shopping-service/docs/README.md)

---

**최종 수정**: 2026-01-30
**버전**: 0.0.1-SNAPSHOT
