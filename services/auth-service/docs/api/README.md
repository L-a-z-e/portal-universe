# 📡 Auth Service API Documentation

> Auth Service API 문서 인덱스

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8081` (로컬) |
| **Version** | v1 |
| **인증** | OAuth2 Authorization Code with PKCE |
| **토큰 형식** | JWT (RS256) |

---

## 📑 API 목록

| API | 설명 | 상태 |
|-----|------|------|
| [Auth API](./auth-api.md) | 인증/인가 및 회원가입 API | ✅ Current |

---

## 🔑 주요 기능

### 인증 (Authentication)
- OAuth2 Authorization Code Flow with PKCE
- JWT 기반 Access Token 및 Refresh Token 발급
- OIDC (OpenID Connect) 지원

### 회원가입 (Registration)
- 이메일 기반 회원가입
- 중복 이메일 검증

### 보안
- Public Client를 위한 PKCE 필수 적용
- State 파라미터를 통한 CSRF 방지
- RS256 알고리즘 기반 JWT 서명

---

## 📘 공통 정보

### 클라이언트 설정

| 항목 | 값 |
|------|-----|
| **Client ID** | `portal-client` |
| **Client Type** | Public Client (Client Secret 없음) |
| **지원 Grant Types** | Authorization Code, Refresh Token |
| **PKCE** | 필수 (S256) |

### 토큰 정책

| 항목 | 값 |
|------|-----|
| **Access Token TTL** | 2분 (120초) |
| **Refresh Token TTL** | 7일 |
| **Refresh Token 재사용** | 불가 (매 갱신 시 새 토큰 발급) |
| **서명 알고리즘** | RS256 |

### 지원 스코프

| 스코프 | 설명 |
|--------|------|
| `openid` | OIDC 표준 (필수) |
| `profile` | 프로필 정보 접근 |
| `read` | 읽기 권한 |
| `write` | 쓰기 권한 |

---

## 🔗 관련 문서

- [System Architecture](../architecture/system-overview.md)
- [Frontend Integration Guide](../guides/frontend-auth-integration.md)
- [API Gateway Configuration](../../api-gateway/docs/api/gateway-api.md)

---

## 📝 마지막 업데이트

| 날짜 | 변경사항 |
|------|----------|
| 2026-01-18 | 최초 API 문서 작성 (auth-api.md) |

---

**최종 업데이트**: 2026-01-18
