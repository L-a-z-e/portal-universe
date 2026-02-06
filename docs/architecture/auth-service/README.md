# Auth Service Architecture

JWT Stateless + Redis 기반 중앙 인증/인가 서비스의 아키텍처 문서입니다.

## Quick Overview

- **인증**: JWT Access Token (15분, Stateless) + Refresh Token (7일, Redis)
- **인가**: RBAC 6개 엔티티 + 서비스별 멤버십 티어
- **보안**: 계정 잠금, 비밀번호 정책 11가지, Token Blacklist, Key Rotation
- **소셜**: Google, Naver, Kakao (조건부 활성화)
- **이벤트**: Kafka `user-signup` (TransactionalEventListener AFTER_COMMIT)

## 문서 목록

| ID | 제목 | 설명 |
|----|------|------|
| [arch-system-overview](./system-overview.md) | System Overview | 전체 아키텍처, 컴포넌트 상세, 데이터 저장소, 에러 코드 |
| [arch-data-flow](./data-flow.md) | Data Flow | 로그인/로그아웃/토큰 갱신/회원가입/소셜 로그인 플로우 |
| [arch-security-mechanisms](./security-mechanisms.md) | Security Mechanisms | 계정 잠금, 비밀번호 정책, RBAC, 토큰 보안, 감사 로그 |

## 읽기 순서

| 목적 | 추천 순서 |
|------|----------|
| 온보딩 (전체 파악) | System Overview → Data Flow → Security Mechanisms |
| 인증 플로우 이해 | Data Flow (2~4절) → Security Mechanisms (4절 토큰 보안) |
| RBAC/권한 이해 | Security Mechanisms (5절) → System Overview (5절 컴포넌트) |
| 보안 감사 | Security Mechanisms 전체 |

## 관련 문서

| 문서 | 경로 |
|------|------|
| Auth API 명세 | [docs/api/auth-service/](../../api/auth-service/README.md) |
| ADR-008: JWT Stateless + Redis | [docs/adr/ADR-008](../../adr/ADR-008-jwt-stateless-redis.md) |
| ADR-003: Admin 권한 검증 전략 | [docs/adr/ADR-003](../../adr/ADR-003-authorization-strategy.md) |
| Architecture 템플릿 | [docs/templates/architecture-template.md](../../templates/architecture-template.md) |

---

## 작성 가이드

- 템플릿: [Architecture 템플릿](../../templates/architecture-template.md)
- 명명 규칙: `arch-[topic].md`
- 다음 ID: arch-004
