# ADR-014: 마이프로필 단계별 구현 전략

**Status**: Accepted
**Date**: 2026-01-21

## Context
Portal Shell에 마이프로필 기능을 추가하려고 하나, 현재 JWT RBAC 작업(ADR-004)이 진행 중입니다. `auth.ts`, `authService.ts` 수정 시 충돌 위험이 있으며, Auth Store에 이미 사용자 프로필 데이터가 로드되어 있습니다.

### 제약 조건
- JWT 작업 진행 중 → Auth 관련 코드 수정 제한
- Auth Store에 `user.profile` 데이터 존재 → API 호출 없이도 조회 가능
- 빠른 기능 제공 필요 → 최소 기능부터 단계적 구현

## Decision
**2단계(Phase) 전략**을 채택하여 마이프로필 기능을 구현합니다.

- **Phase 1 (즉시)**: 읽기 전용 프로필 (Auth Store 데이터만 사용, API 호출 없음)
- **Phase 2 (JWT 작업 완료 후)**: 프로필 수정/삭제 기능 (REST API + JWT 인증)

## Rationale
- JWT 작업과 격리하여 충돌 위험 제거
- Phase 1을 1-2일 내 빠르게 제공
- 새 컴포넌트로 구현하여 기존 코드 보호
- Auth Store의 공개 API만 사용 (낮은 결합도)
- 단계별 검증으로 버그 조기 발견

## Trade-offs
✅ **장점**:
- 리스크 최소화 (기존 인증 로직 안정성 유지)
- 빠른 가치 제공 (1-2일 내 핵심 기능 제공)
- 코드 품질 (격리된 컴포넌트, 낮은 결합도)
- 유연성 (Phase 2 일정 조정 가능)

⚠️ **단점 및 완화**:
- 불완전한 UX (Phase 1에서 수정 불가) → (완화: "프로필 수정 기능은 곧 추가됩니다" 안내 메시지)
- 2단계 배포 필요 → (완화: Phase 1은 Frontend만 배포, Phase 2는 JWT PR과 함께 배포)
- 데이터 동기화 (재로그인 시에만 최신 정보 반영) → (완화: Phase 2에서 API 호출로 해결)

## Implementation
### Phase 1 (1-2일)
- `MyProfilePage.vue`: 프로필 페이지 (신규 생성)
- `components/profile/*`: 프로필 컴포넌트 (신규 생성)
- `router/index.ts`: 라우트 추가
- Auth Store 읽기만 사용 (수정 금지)

### Phase 2 (5-7일, JWT 작업 완료 후)
- `PATCH /api/auth/profile`: 프로필 수정 API
- `POST /api/auth/password/change`: 비밀번호 변경
- `DELETE /api/auth/account`: 회원 탈퇴
- `useProfile.ts`: API 연동 및 Auth Store 갱신

## References
- [SCENARIO-003: 마이프로필 조회 및 관리](../scenarios/SCENARIO-003-my-profile.md)
- [ADR-004: JWT RBAC 자동 설정 전략](./ADR-004-jwt-rbac-auto-configuration.md)

---

📂 상세: [old-docs/central/adr/ADR-014-my-profile-phased-approach.md](../old-docs/central/adr/ADR-014-my-profile-phased-approach.md)
