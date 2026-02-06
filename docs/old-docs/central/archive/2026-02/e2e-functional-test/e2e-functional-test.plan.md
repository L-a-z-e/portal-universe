# Plan: E2E Functional Test v2.0 - Portal Universe

> PDCA Phase: Plan
> Feature: e2e-functional-test
> Created: 2026-02-03
> Status: Draft (v2.0 - Restructured)

## 1. Background & Motivation

### 1.1 이전 상태 분석

| 항목 | 상태 | 비고 |
|-----|------|-----|
| 기존 Plan (v1.0) | 폐기 | e2e-tests/ 기반 - 현재 구조와 불일치 |
| 이전 실행 결과 | 32% pass | 147개 중 47개 통과 |
| 주요 실패 원인 | data-testid 미구현 | 테스트 코드와 UI 불일치 |

### 1.2 현재 E2E 테스트 구조

```
Portal Universe E2E 테스트 분포
├── e2e-tests/                    # 통합 E2E (37 files)
│   ├── tests/auth-service/       # 인증 테스트
│   ├── tests/blog/               # Blog 서비스
│   ├── tests/shopping/           # Shopping 서비스
│   ├── tests/prism/              # Prism 서비스
│   └── tests/admin/              # Admin 관리
│
├── frontend/e2e/                 # Portal Shell 레벨 (4 files)
│   ├── portal/                   # 네비게이션, 테마
│   ├── blog/                     # Blog 기본
│   └── shopping/                 # Shopping 기본
│
└── frontend/blog-frontend/e2e/   # Blog 독립 테스트 (9 files)
    └── tests/                    # 상세 기능 테스트
```

### 1.3 문제점

1. **data-testid 불일치**
   - 테스트에서 사용: 20+ (comment-*, author-*, like-* 등)
   - 실제 구현: 8개 (feed 관련만)

2. **테스트 위치 분산**
   - 통합 테스트 (`e2e-tests/`)와 개별 테스트 (`frontend/*/e2e/`) 혼재
   - 실행 환경 및 baseURL 차이

3. **인증 fixture 문제**
   - localStorage 접근 시 SecurityError 발생
   - 테스트 환경과 실제 환경 차이

## 2. Goals

### 2.1 Primary Goals

| Goal | 측정 기준 | Target |
|------|----------|--------|
| E2E Pass Rate | 전체 테스트 통과율 | >= 90% |
| data-testid 커버리지 | 테스트 vs 구현 일치율 | 100% |
| 테스트 구조 정리 | 단일 진입점 | 1개 config |

### 2.2 Scope

| In Scope | Out of Scope |
|----------|--------------|
| frontend/e2e/ 테스트 정상화 | e2e-tests/ 통합 테스트 (별도 PDCA) |
| blog-frontend/e2e/ 테스트 정상화 | shopping-frontend E2E 신규 작성 |
| data-testid 추가 | prism-frontend E2E 신규 작성 |
| auth fixture 수정 | 성능 테스트 |

## 3. Current Test Status

### 3.1 frontend/e2e/ (Portal Shell)

| 테스트 파일 | 테스트 수 | 상태 | 비고 |
|------------|----------|------|-----|
| portal/navigation.spec.ts | 4 | ✅ Pass | 네비게이션 동작 |
| portal/theme-toggle.spec.ts | 3 | ⏭️ Skip | data-testid 필요 |
| blog/post-list.spec.ts | 3 | ✅ Pass | 기본 동작 |
| shopping/product-list.spec.ts | 4 | ✅ Pass | 기본 동작 |

**결과**: 9 passed / 5 skipped (64%)

### 3.2 blog-frontend/e2e/ (Blog 독립)

| 테스트 파일 | 테스트 수 | 상태 | 주요 실패 원인 |
|------------|----------|------|--------------|
| comment.spec.ts | 14 | ❌ 대부분 실패 | data-testid 미구현 |
| like.spec.ts | 8 | ❌ 대부분 실패 | data-testid 미구현 |
| follow.spec.ts | 19 | ❌ 대부분 실패 | data-testid 미구현 |
| feed.spec.ts | 21 | 🔶 일부 통과 | feed 관련만 구현됨 |
| my-page.spec.ts | 24 | ❌ 대부분 실패 | data-testid 미구현 |
| series.spec.ts | 7 | ❌ 대부분 실패 | data-testid 미구현 |
| tag.spec.ts | 10 | ❌ 대부분 실패 | data-testid 미구현 |
| trending.spec.ts | 13 | 🔶 일부 통과 | 기본 기능만 통과 |
| user-blog.spec.ts | 17 | ❌ 대부분 실패 | data-testid 미구현 |

**결과**: 38 passed / 95 failed (29%)

### 3.3 필요한 data-testid 목록

```typescript
// Comment 관련
'comment-section', 'comment-list', 'comment-item', 'comment-form',
'comment-input', 'comment-submit-btn', 'comment-reply-btn',
'comment-edit-btn', 'comment-delete-btn', 'comment-author'

// Like 관련
'like-button', 'like-count', 'liked-button'

// Follow 관련
'follow-button', 'follower-count', 'following-count',
'follower-modal', 'following-modal'

// My Page 관련
'profile-info', 'profile-edit-btn', 'my-posts-list',
'post-status-filter', 'post-delete-btn', 'post-publish-btn'

// Series 관련
'series-list', 'series-item', 'series-navigation'

// Tag 관련
'tag-list', 'tag-item', 'tag-search-input', 'tag-sort-select'

// User Blog 관련
'user-profile', 'user-posts', 'user-social-links'
```

## 4. Implementation Strategy

### 4.1 Phase 1: Auth Fixture 수정

**목표**: localStorage 접근 오류 해결

```typescript
// before (문제)
await page.evaluate(() => localStorage.clear())

// after (해결)
await page.goto('http://localhost:30001')  // 먼저 페이지 이동
await page.evaluate(() => localStorage.clear())
```

### 4.2 Phase 2: data-testid 추가

**우선순위 기준**:
1. 테스트 커버리지가 높은 컴포넌트
2. 핵심 사용자 플로우 (comment, like, follow)
3. 구현 복잡도 낮은 것 우선

**작업 대상 컴포넌트**:

| 컴포넌트 | 위치 | 추가할 data-testid 수 |
|---------|-----|---------------------|
| CommentSection.vue | blog-frontend/src/components/ | 15 |
| LikeButton.vue | blog-frontend/src/components/ | 3 |
| FollowButton.vue | blog-frontend/src/components/ | 3 |
| MyPage.vue | blog-frontend/src/views/ | 10 |
| SeriesList.vue | blog-frontend/src/components/ | 5 |
| TagList.vue | blog-frontend/src/components/ | 5 |
| UserBlog.vue | blog-frontend/src/views/ | 5 |

### 4.3 Phase 3: 테스트 재실행 및 검증

1. auth fixture 수정 후 테스트 실행
2. data-testid 추가 후 테스트 실행
3. 실패 케이스 분석 및 추가 수정

## 5. Success Criteria

| Metric | Current | Target |
|--------|---------|--------|
| frontend/e2e/ Pass Rate | 64% | 100% |
| blog-frontend/e2e/ Pass Rate | 29% | >= 85% |
| data-testid Coverage | 40% | 100% |
| Auth Fixture Error | 있음 | 없음 |

## 6. Deliverables

| Deliverable | Description |
|-------------|-------------|
| Auth Fixture 수정 | localStorage 접근 오류 해결 |
| data-testid 추가 | 46개 컴포넌트 속성 추가 |
| 테스트 결과 리포트 | Pass/Fail 매트릭스 |
| Design 문서 | 상세 구현 명세 |

## 7. Risk & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| 컴포넌트 구조 변경 | data-testid 추가 시 side effect | 기존 테스트 먼저 실행하여 검증 |
| 테스트 데이터 부재 | 일부 기능 테스트 불가 | Mock 데이터 또는 seed 사용 |
| 인증 상태 유지 | 테스트 간 상태 누수 | beforeEach에서 초기화 |

## 8. Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| Phase 1 | 1일 | Auth Fixture 수정 |
| Phase 2 | 2-3일 | data-testid 추가 (46개) |
| Phase 3 | 1일 | 테스트 재실행 및 검증 |
| **Total** | **4-5일** | |

## 9. Related Documents

| Document | Status |
|----------|--------|
| [e2e-test-refactoring.report.md](../../archive/2026-02/e2e-test-refactoring/) | ✅ Archived (96%) |
| [e2e-test-fix.report.md](../../archive/2026-02/e2e-test-fix/) | ✅ Archived (100%) |
| e2e-functional-test.design.md | 📝 To be created |

---

**Version History**

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-01 | Initial plan (e2e-tests/ 기반) |
| 2.0 | 2026-02-03 | Restructured (현재 테스트 구조 기반) |
