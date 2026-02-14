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
| [온보딩 패스](./onboarding-path.md) | 단계별 학습 경로 | 신규 개발자 |
| [Polyglot 신규 서비스 체크리스트](./new-service-checklist.md) | 스택 무관 cross-cutting 체크리스트 | 신규 서비스 추가 시 |

---

## Design System

> @portal/design-system 사용을 위한 개발자 가이드 문서입니다.

| 문서 | 설명 |
|------|------|
| [Design Tokens 시작하기](./getting-started.md) | Design Tokens 설치, CSS 변수, Tailwind 통합 |
| [컴포넌트 사용 가이드](./using-components.md) | Vue 컴포넌트 사용법 |
| [테마 적용 가이드](./theming-guide.md) | Light/Dark 모드, 서비스별 테마 |
| [Design System 아키텍처](./design-system-architecture.md) | 구조 및 설계 원칙 |
| [기여 가이드](./contributing.md) | 새 컴포넌트 추가 방법 |
| [Storybook](./storybook.md) | 컴포넌트 카탈로그 사용법 |

---

## 보안 및 인증

| 문서 | 설명 |
|------|------|
| [JWT RBAC 설정](./jwt-rbac-setup.md) | JWT 기반 역할 권한 설정 + 로그인 보안 |
| [RBAC 리팩토링](./rbac-refactoring.md) | 역할 기반 접근 제어 개선 |
| [보안 모듈](./security-module.md) | 보안 관련 공통 모듈 |
| [보안 헤더](./security-headers.md) | HTTP 보안 헤더 구현 및 검증 |
| [보안 감사 로그](./security-audit-log-setup.md) | 감사 로그 구현 및 설정 |
| [Swagger/Actuator 보안](./swagger-actuator-security.md) | API 문서 및 모니터링 엔드포인트 보호 |
| [Rate Limiting](./rate-limiting.md) | API Gateway Redis 기반 요청 제한 |

---

## 프론트엔드

| 문서 | 설명 |
|------|------|
| [Module Federation 통합 가이드](./module-federation-guide.md) | Remote 추가/통합/트러블슈팅 |
| [빌드 파이프라인](./frontend-build-pipeline.md) | 프론트엔드 빌드 프로세스 |
| [Portal Shell 워크플로우](./portal-shell-workflow.md) | Portal Shell 개발 워크플로우 (Vue 3) |
| [Admin 가이드](./admin-guide.md) | Shopping Admin 접근/권한/상품 관리/개발 레퍼런스 |
| [쿠폰/타임딜 가이드](./coupon-timedeal-guide.md) | 프로모션 기능 |

---

## 백엔드

| 문서 | 설명 |
|------|------|
| [Common Library 사용법](./common-library-usage.md) | 공유 라이브러리 활용 |
| [Kafka 이벤트](./kafka-events.md) | 이벤트 기반 통신 |
| [Notification 로컬 개발](./notification-local-dev.md) | Notification Service 로컬 개발 (Kafka, Email) |

---

## 관련 문서

- [아키텍처 문서](../../architecture/) - 시스템 및 서비스 아키텍처
- [API 문서](../../api/) - REST API 명세
- [ADR](../../adr/) - 아키텍처 결정 기록
- [Troubleshooting](../../troubleshooting/) - 문제 해결 기록

---

작성자: Laze
