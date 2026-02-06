---
id: guide-development-index
title: 개발 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [development, guide, index]
---

# 개발 가이드

> Portal Universe 프로젝트 개발을 위한 가이드 문서 모음입니다.

---

## 시작하기

| 문서 | 설명 | 대상 |
|------|------|------|
| [로컬 개발 환경 구성](./local-dev-setup.md) | Zero to Running 전체 환경 구성 | 신규 개발자 |
| [Notification 로컬 개발](./notification-local-dev.md) | Notification Service 로컬 개발 | 서비스 개발자 |
| [온보딩 패스](./onboarding-path.md) | 단계별 학습 경로 | 신규 개발자 |
| [빠른 시작 가이드](./getting-started.md) | 기본 설정 및 도구 사용법 | 신규 개발자 |

---

## Design System

> @portal/design-system 사용을 위한 개발자 가이드 문서입니다.

---

| 문서 | 설명 |
|------|------|
| [컴포넌트 사용 가이드](./using-components.md) | Vue 컴포넌트 사용법 |
| [테마 적용 가이드](./theming-guide.md) | Light/Dark 모드, 서비스별 테마 |
| [Design System 아키텍처](./design-system-architecture.md) | 구조 및 설계 원칙 |
| [기여 가이드](./contributing.md) | 새 컴포넌트 추가 방법 |
| [Storybook](./storybook.md) | 컴포넌트 카탈로그 사용법 |

---

## 보안 및 인증

| 문서 | 설명 |
|------|------|
| [JWT RBAC 설정](./jwt-rbac-setup.md) | JWT 기반 역할 권한 설정 |
| [RBAC 리팩토링](./rbac-refactoring.md) | 역할 기반 접근 제어 개선 |
| [보안 구현 스펙](./security-implementation-spec.md) | 보안 기능 구현 명세 |
| [보안 모듈](./security-module.md) | 보안 관련 공통 모듈 |
| [보안 헤더 테스트](./security-headers-testing.md) | HTTP 보안 헤더 검증 |
| [보안 감사 로그](./security-audit-log-setup.md) | 감사 로그 설정 |
| [Swagger/Actuator 보안](./swagger-actuator-security.md) | API 문서 및 모니터링 엔드포인트 보호 |

---

## Shopping/Admin 기능

| 문서 | 설명 |
|------|------|
| [Admin UI 가이드](./admin-ui-guide.md) | 관리자 인터페이스 |
| [Admin 상품 가이드](./admin-product-guide.md) | 상품 관리 기능 |
| [쿠폰/타임딜 가이드](./coupon-timedeal-guide.md) | 프로모션 기능 |
| [Rate Limiting](./rate-limiting.md) | API 요청 제한 |

---

## 프론트엔드 아키텍처

| 문서 | 설명 |
|------|------|
| [Module Federation 통합](./federation-integration.md) | MFE 통합 가이드 |
| [Remote 앱 추가](./adding-remote.md) | 새 Remote 앱 연결 |
| [빌드 파이프라인](./frontend-build-pipeline.md) | 프론트엔드 빌드 프로세스 |

---

## 백엔드 아키텍처

| 문서 | 설명 |
|------|------|
| [Common Library 사용법](./common-library-usage.md) | 공유 라이브러리 활용 |
| [Kafka 이벤트](./kafka-events.md) | 이벤트 기반 통신 |

---

## 기타

| 문서 | 설명 |
|------|------|
| [Portal Shell 워크플로우](./portal-shell-workflow.md) | Portal Shell 개발 워크플로우 |

---

## 관련 문서

- [테스트 문서](../../testing/) - 테스트 전략 및 계획
- [아키텍처 문서](../../architecture/) - 시스템 및 서비스 아키텍처
- [API 문서](../../api/) - REST API 명세
- [ADR](../../adr/) - 아키텍처 결정 기록
- [Troubleshooting](../../troubleshooting/) - 문제 해결 기록

---

작성자: Laze
