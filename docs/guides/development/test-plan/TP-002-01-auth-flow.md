---
id: TP-002-01
title: Auth Flow 테스트 계획
type: test-plan
status: draft
created: 2026-01-19
updated: 2026-01-19
related:
  - PRD-001
---

# Auth Flow 테스트 계획

## 개요

| 항목 | 내용 |
|------|------|
| **관련 PRD** | [PRD-001-ecommerce-core](../../prd/PRD-001-ecommerce-core.md) |
| **테스트 범위** | 인증/인가 흐름 전체 |
| **테스트 환경** | Local, CI, Staging |

## 테스트 목표

1. 회원가입/로그인 흐름 검증
2. JWT 토큰 발급 및 검증
3. 권한(Role) 기반 접근 제어 검증
4. 토큰 갱신 및 로그아웃 검증

## 테스트 범위

### In Scope

- 회원가입 API
- 로그인 API
- 토큰 갱신 API
- 로그아웃 API
- 권한 검증 (ROLE_USER, ROLE_ADMIN)
- API Gateway 인증 필터

### Out of Scope

- OAuth 소셜 로그인 (별도 계획)
- 2FA (미구현)

## 테스트 케이스 요약

### 회원가입

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-002-01-01 | 정상 회원가입 | P1 | Pending |
| TC-002-01-02 | 중복 이메일로 가입 시도 | P1 | Pending |
| TC-002-01-03 | 필수 필드 누락 | P2 | Pending |
| TC-002-01-04 | 비밀번호 정책 위반 | P2 | Pending |

### 로그인

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-002-01-10 | 정상 로그인 | P1 | Pending |
| TC-002-01-11 | 잘못된 비밀번호 | P1 | Pending |
| TC-002-01-12 | 존재하지 않는 이메일 | P1 | Pending |
| TC-002-01-13 | 비활성 계정 로그인 시도 | P2 | Pending |

### JWT 토큰

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-002-01-20 | Access Token으로 API 접근 | P1 | Pending |
| TC-002-01-21 | 만료된 Access Token | P1 | Pending |
| TC-002-01-22 | Refresh Token으로 Access Token 갱신 | P1 | Pending |
| TC-002-01-23 | 만료된 Refresh Token | P1 | Pending |
| TC-002-01-24 | 무효화된 Refresh Token 사용 | P1 | Pending |

### 권한 검증

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-002-01-30 | ADMIN이 Admin API 접근 | P1 | Pending |
| TC-002-01-31 | USER가 Admin API 접근 시도 | P1 | Pending |
| TC-002-01-32 | 인증 없이 보호된 API 접근 | P1 | Pending |
| TC-002-01-33 | 공개 API 접근 (토큰 없이) | P2 | Pending |

### 로그아웃

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-002-01-40 | 정상 로그아웃 | P1 | Pending |
| TC-002-01-41 | 로그아웃 후 Refresh Token 사용 | P1 | Pending |

## 합격 기준

| 항목 | 기준 |
|------|------|
| P1 테스트 통과율 | 100% |
| P2 테스트 통과율 | 95% |
| 보안 취약점 | 0건 |

## 테스트 데이터

### 사전 조건 데이터

```sql
-- Admin User
INSERT INTO users (id, email, password, username, status)
VALUES (1, 'admin@test.com', '$2a$10$...', 'Admin', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
VALUES (1, (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'));

-- Regular User
INSERT INTO users (id, email, password, username, status)
VALUES (2, 'user@test.com', '$2a$10$...', 'User', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
VALUES (2, (SELECT id FROM roles WHERE name = 'ROLE_USER'));
```

## 테스트 실행

### 로컬 실행

```bash
cd services/auth-service
./gradlew test
```

### Integration Test

```bash
./gradlew integrationTest
```

## 관련 문서

- [테스트 전략](../test-strategy.md)
- [Architecture - Identity Model](../../architecture/system/identity-model.md)
- [Diagrams - Auth ERD](../../diagrams/source/auth-erd.md)
