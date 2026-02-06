# ADR-009: Settings Page 아키텍처 설계

**Status**: Accepted
**Date**: 2026-01-21

## Context

Portal Shell에 사용자 설정 페이지 추가가 필요합니다. 테마(Dark/Light/System), 언어, 알림 수신 설정을 지원하며, 비회원도 사용 가능해야 하고 네트워크 실패에도 동작해야 합니다. 기존 `theme.ts` store를 최대한 활용하되 JWT/인증 로직 수정을 최소화해야 합니다.

## Decision

**Local-First with Optional Sync** 방식을 채택합니다. localStorage를 primary로 사용하고, 백엔드는 선택적 동기화 레이어로 활용합니다.

## Rationale

- **즉시 반영**: localStorage 읽기/쓰기로 빠른 응답성 확보 (< 1ms)
- **오프라인 지원**: 네트워크 없이도 모든 설정 동작
- **비회원 지원**: 로그인 없이도 설정 가능, UX 향상
- **회원 동기화**: 인증된 사용자는 Best Effort 동기화로 다중 디바이스 지원
- **장애 허용**: 백엔드 API 실패 시에도 기능 정상 동작

## Trade-offs

✅ **장점**:
- 빠른 응답, 오프라인 동작, 비회원/회원 모두 지원
- 기존 theme.ts 로직 재사용, JWT 독립적
- 네트워크 지연/실패 영향 없음

⚠️ **단점 및 완화**:
- 백엔드 동기화 실패 시 디바이스 간 일시적 불일치 → (완화: 다음 로그인 시 백엔드 설정으로 동기화)
- localStorage 접근 불가 시 설정 손실 → (완화: 기본값 제공, 세션 메모리 사용)
- Auth Service에 `user_settings` 테이블 추가 필요 → (완화: 간단한 스키마, 복잡도 낮음)

## Implementation

**Store 구조**:
- `stores/settings.ts` (신규): 통합 설정 관리, localStorage 저장, 선택적 백엔드 동기화
- `stores/theme.ts` (확장): 기존 기능 유지

**백엔드 API** (선택적):
- `GET/PUT /api/auth/users/me/settings`
- `user_settings` 테이블 (1:1 User 매핑)

**데이터 흐름**:
1. 설정 변경 → 즉시 localStorage 저장 (동기)
2. 인증된 경우 → 백엔드 동기화 시도 (비동기, Silent Fail)
3. 앱 초기화 → localStorage 로드 → 인증 시 백엔드 설정 우선 적용

**주요 파일**:
- `frontend/portal-shell/src/store/settings.ts`
- `frontend/portal-shell/src/pages/SettingsPage.vue`
- `services/auth-service/.../entity/UserSettings.java`

## References

- [SCENARIO-007 Settings Page](../scenarios/SCENARIO-007-settings-page.md)
- [ADR-005 민감 데이터 관리 전략](./ADR-005-sensitive-data-management.md)
- [Local-First Software Principles](https://www.inkandswitch.com/local-first/)

---

📂 상세: [old-docs/central/adr/ADR-009-settings-page-architecture.md](../old-docs/central/adr/ADR-009-settings-page-architecture.md)
