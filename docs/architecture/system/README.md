# System Architecture

> Portal Universe의 시스템 레벨 아키텍처 문서

**마지막 업데이트**: 2026-02-06

---

## 개요

이 디렉토리는 특정 서비스가 아닌 **시스템 전체**에 적용되는 아키텍처를 다룹니다.

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [Polyglot Overview](polyglot-overview.md) | Polyglot 전체 조감도 (기술 스택, cross-cutting concern 매트릭스) |
| [Identity Model](identity-model.md) | 사용자 식별 체계 (Internal/External ID, Identity-Profile 분리) |
| [Security Architecture](security-architecture.md) | 시스템 보안 아키텍처 (JWT, RBAC, Gateway, OAuth2) |
| [Common Library](common-library.md) | 공유 라이브러리 (Response, Exception, Security, Audit) |
| [Event-Driven Architecture](event-driven-architecture.md) | Kafka 기반 이벤트 아키텍처 (17개 토픽) |
| [Service Communication](service-communication.md) | 서비스 간 통신 패턴 (Gateway, Feign, 헤더 전파) |

---

## 카테고리

### 인증/인가
- [Identity Model](identity-model.md): 사용자 식별 체계, Internal/External ID 전략
- [Security Architecture](security-architecture.md): JWT HMAC-SHA256, RBAC, OAuth2
- 관련 서비스: auth-service

### 서비스 간 통신
- [Service Communication](service-communication.md): Gateway 라우팅, Feign Client, 헤더 전파, Circuit Breaker
- [Event-Driven Architecture](event-driven-architecture.md): Kafka 기반 비동기 통신
- 관련 서비스: 전체

### 공통 라이브러리
- [Common Library](common-library.md): Response/Exception/Security/Audit/Utility 레이어
- 관련 서비스: 전체 Java/Spring 서비스

---

## 관련 문서
- [전체 문서 포털](../../README.md)
- [서비스별 아키텍처](../)
- [ADR 목록](../../adr/_INDEX.md)

---

📂 시스템 레벨 설계 결정은 [ADR](../../adr/) 참조
