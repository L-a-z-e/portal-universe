# Architecture Documentation

Blog Frontend 아키텍처 문서 목록입니다.

---

## 문서 목록

| ID | 문서 | 상태 | 마지막 업데이트 | 설명 |
|----|------|------|-----------------|------|
| arch-system-overview | [System Overview](./system-overview.md) | ✅ Current | 2026-02-06 | 시스템 전체 구조 및 핵심 특징 |
| arch-data-flow | [Data Flow](./data-flow.md) | ✅ Current | 2026-02-06 | 데이터 흐름, API 통신, 인증, Pinia 상태 관리 |
| arch-module-federation | [Module Federation](./module-federation.md) | ✅ Current | 2026-02-06 | Module Federation 상세 설정 및 통합 가이드 |

---

## 주제별 참조 가이드

아래 주제들은 기존 문서에 통합되어 있습니다.

| 주제 | 참조 문서 | 섹션 |
|------|-----------|------|
| Standalone/Embedded Dual Mode | [system-overview.md](./system-overview.md) | Dual Mode Architecture, Entry Points |
| Router Architecture & Auth Guard | [system-overview.md](./system-overview.md) | Router Configuration |
| Theme System & Dark Mode | [system-overview.md](./system-overview.md) | Theme & Styling |
| State Management (Pinia) | [data-flow.md](./data-flow.md) | Pinia 상태 관리 흐름 |

---

## 관련 리소스

- [Blog Service API Documentation](../../api/blog-service/)
- [Blog Service Architecture](../blog-service/)
- [Auth Service Architecture](../auth-service/)
- [Portal Shell Architecture](../portal-shell/)

---

**최종 업데이트**: 2026-02-06
