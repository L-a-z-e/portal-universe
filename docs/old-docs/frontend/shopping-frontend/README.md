---
id: shopping-frontend-docs
title: Shopping Frontend Documentation
type: index
status: current
created: 2026-01-18
updated: 2026-01-30
author: Laze
tags: [shopping-frontend, documentation, index]
---

# Shopping Frontend 문서 포털

> Portal Universe - Shopping Frontend 기술 문서 통합 인덱스

---

## 📋 개요

Shopping Frontend는 React 18 + TypeScript + Vite 기반의 마이크로 프론트엔드 모듈입니다. Module Federation을 통해 Portal Shell에 통합되며, 전자상거래 기능(상품 관리, 장바구니, 주문/결제)을 제공합니다.

> ⚠️ **현재 개발 상태**: 기본 구조와 부트스트랩 함수는 구현되었으나, 일부 기능은 아직 개발 중입니다.

---

## 🚀 빠른 시작

| 단계 | 문서 | 설명 |
|------|------|------|
| 1️⃣ | [Getting Started](./guides/getting-started.md) | 개발 환경 설정 및 실행 |
| 2️⃣ | [Architecture](./architecture/) | 시스템 구조 이해 |
| 3️⃣ | [API 문서](./api/) | Backend API 연동 |
| 4️⃣ | [Module Federation 통합](./guides/federation-integration.md) | Portal Shell 통합 가이드 |

---

## 📚 문서 유형별 인덱스

### 🗺️ [Guides - 개발자 가이드](./guides/)

| 문서 | 상태 | 설명 |
|------|------|------|
| [Module Federation 통합](./guides/federation-integration.md) | ✅ Current | Portal Shell과의 통합 방법 |
| Getting Started | 🔜 예정 | 개발 환경 설정 및 실행 |

### 🏗️ [Architecture - 아키텍처](./architecture/)

| 문서 | 상태 | 설명 |
|------|------|------|
| [System Overview](./architecture/system-overview.md) | ✅ Current | React 18 Module Federation Remote 구조 |

### 📡 [API - API 명세서](./api/)

| 문서 | 상태 | 설명 |
|------|------|------|
| Product API | 🔜 예정 | 상품 CRUD API |
| Cart API | 🔜 예정 | 장바구니 API |
| Order API | 🔜 예정 | 주문 API |

---

## 🚦 서비스 URL (로컬 개발)

| 환경 | URL | 설명 |
|------|-----|------|
| **Development** | http://localhost:30002 | Standalone 개발 모드 |
| **Portal Shell** | http://localhost:30000/shopping | Portal에 통합된 상태 |
| **Backend API** | http://localhost:8080/api/v1/shopping | Shopping Service API |

---

## 🔗 관련 리소스

### 다른 프론트엔드 모듈
- [Portal Shell 문서](../../portal-shell/docs/)
- [Blog Frontend 문서](../../blog-frontend/docs/)
- [Design System 문서](../../design-system-vue/docs/)

### 백엔드 서비스
- [Shopping Service 문서](../../../services/shopping-service/docs/)
- [Auth Service 문서](../../../services/auth-service/docs/)
- [API Gateway 문서](../../../services/api-gateway/docs/)

---

**최종 업데이트**: 2026-01-30
