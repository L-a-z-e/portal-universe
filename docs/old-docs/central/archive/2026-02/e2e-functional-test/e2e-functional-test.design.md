# E2E Functional Test Design Document

> **Summary**: E2E 테스트 정상화를 위한 Semantic Selector 기반 테스트 설계
>
> **Project**: Portal Universe
> **Version**: 3.0
> **Author**: Claude
> **Date**: 2026-02-03
> **Status**: Final
> **Planning Doc**: [e2e-functional-test.plan.md](../01-plan/features/e2e-functional-test.plan.md)

---

## 1. Overview

### 1.1 Design Goals

1. **E2E 테스트 Pass Rate >= 90% 달성**
2. **Semantic Selector 100% 커버리지** - data-testid 없이 CSS 클래스 + Playwright API 사용
3. **Auth Fixture 안정화** - localStorage 접근 오류 해결
4. **테스트 실행 환경 단일화** - 명확한 실행 방법 문서화

### 1.2 Design Principles

- **No data-testid**: 별도 테스트 속성 없이 기존 CSS 클래스와 semantic HTML 활용
- **Playwright Best Practices**: getByRole, getByText, getByLabel 우선 사용
- **Fallback Strategy**: CSS 클래스 조합으로 안정적 selector 구성
- **Non-invasive**: 프로덕션 코드 변경 최소화

---

## 2. Selector 전략

### 2.1 우선순위 (Playwright 권장)

| 순위 | 방식 | 예시 | 사용 상황 |
|:----:|------|------|----------|
| 1 | getByRole | `getByRole('button', { name: /등록/i })` | 버튼, 링크, 폼 요소 |
| 2 | getByText | `getByText('댓글 작성')` | 텍스트 기반 요소 |
| 3 | getByLabel | `getByLabel('이메일')` | 폼 입력 필드 |
| 4 | CSS Class | `.comment-section`, `.post-card` | 컨테이너, 리스트 |
| 5 | Semantic HTML | `article`, `section`, `nav` | 구조적 요소 |

### 2.2 Fallback 패턴

```typescript
// Primary: getByRole → Fallback: CSS class
const submitButton = page
  .getByRole('button', { name: /등록|submit/i })
  .or(page.locator('.submit-btn, button[type="submit"]'))

// Multiple CSS class options
const commentItem = page.locator('.comment-item, .comment')
```

---

## 3. Component Selector 명세

### 3.1 Comment 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 댓글 영역 | `.comment-section, .comments` | CommentList.vue |
| 댓글 목록 | `.comment-list, .comments-list` | CommentList.vue |
| 개별 댓글 | `.comment-item, .comment` | CommentItem.vue |
| 작성자 | `.comment-author, .author` | CommentItem.vue |
| 내용 | `.comment-content, .content` | CommentItem.vue |
| 시간 | `.comment-timestamp, .timestamp, time` | CommentItem.vue |
| 댓글 폼 | `.comment-form, form.comment` | CommentForm.vue |
| 입력창 | `textarea, input[type="text"]` + getByRole('textbox') | CommentForm.vue |
| 등록 버튼 | `button[type="submit"]` + getByRole('button', { name: /등록|submit|작성/i }) | CommentForm.vue |
| 답글 버튼 | `.reply-btn, .reply-button` + getByRole('button', { name: /답글|reply/i }) | CommentItem.vue |
| 수정 버튼 | `.edit-btn, .edit-button` + getByRole('button', { name: /수정|edit/i }) | CommentItem.vue |
| 삭제 버튼 | `.delete-btn, .delete-button` + getByRole('button', { name: /삭제|delete/i }) | CommentItem.vue |

### 3.2 Like 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 좋아요 버튼 | `.like-button, .like-btn` + getByRole('button') | LikeButton.vue |
| 좋아요 수 | `.like-count, .likes-count` | LikeButton.vue |
| 활성화 상태 | `.liked, .is-liked, [aria-pressed="true"]` | LikeButton.vue |

### 3.3 Follow 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 팔로우 버튼 | `.follow-button, .follow-btn` + getByRole('button', { name: /팔로우|follow/i }) | FollowButton.vue |
| 팔로워 수 | `.follower-count, .followers` | UserProfileCard.vue |
| 팔로잉 수 | `.following-count, .following` | UserProfileCard.vue |
| 팔로워 모달 | `.follower-modal, [role="dialog"]` | FollowerModal.vue |

### 3.4 My Page 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 프로필 영역 | `.profile-info, .user-profile` | MyPage.vue |
| 프로필 수정 버튼 | getByRole('button', { name: /프로필 수정|edit profile/i }) | MyPage.vue |
| 내 글 목록 | `.my-posts, .posts-list` | MyPostList.vue |
| 글 상태 필터 | `.post-filter, select` + getByRole('combobox') | MyPostList.vue |
| 글 작성 버튼 | getByRole('button', { name: /새 글|작성|write/i }) | MyPage.vue |
| 개별 글 | `.post-item, .post-card, article` | MyPostList.vue |

### 3.5 Series 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 시리즈 목록 | `.series-list, .my-series` | MySeriesList.vue |
| 시리즈 아이템 | `.series-item, .series-card` | SeriesCard.vue |
| 시리즈 네비게이션 | `.series-nav, .series-navigation` | SeriesBox.vue |
| 시리즈 글 목록 | `.series-posts` | SeriesDetailPage.vue |

### 3.6 Tag 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 태그 목록 | `.tag-list, .tags` | TagListPage.vue |
| 태그 아이템 | `.tag-item, .tag` + getByRole('link') | TagListPage.vue |
| 태그 검색 | `input[type="search"]` + getByRole('searchbox') | TagListPage.vue |

### 3.7 User Blog 관련

| 요소 | Selector | 컴포넌트 |
|------|----------|---------|
| 사용자 프로필 | `.user-profile, .profile-card` | UserBlogPage.vue |
| 사용자 글 목록 | `.user-posts, .posts` | UserBlogPage.vue |
| 작성자 이름 | `.author-name, .author` | PostCard.vue |

---

## 4. Auth Fixture 설계

### 4.1 현재 구현 (addInitScript 방식)

```typescript
// blog-frontend/e2e/fixtures/auth.ts

async function ensureValidPage(page: Page) {
  const currentUrl = page.url()
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto(process.env.BASE_URL || 'http://localhost:30001')
  }
}

export async function mockLogin(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('auth_token', 'mock-jwt-token')
    localStorage.setItem('user', JSON.stringify({
      id: 'test-user-1',
      username: 'testuser',
      email: 'test@example.com',
      nickname: 'Test User',
    }))
  })
}

export async function mockLogout(page: Page) {
  await ensureValidPage(page)
  await page.evaluate(() => {
    localStorage.clear()
  })
}
```

### 4.2 장점

- `addInitScript`: 페이지 로드 전에 실행되어 안정적
- URL 검증 후 localStorage 접근으로 SecurityError 방지

---

## 5. 테스트 파일 구조

```
blog-frontend/e2e/
├── fixtures/
│   └── auth.ts              ✅ localStorage 안전 접근
├── tests/
│   ├── comment.spec.ts      ✅ CSS class + getByRole
│   ├── like.spec.ts         ✅ CSS class + getByRole
│   ├── follow.spec.ts       ✅ CSS class + getByRole
│   ├── my-page.spec.ts      ✅ CSS class + getByRole
│   ├── series.spec.ts       ✅ CSS class + getByRole
│   ├── tag.spec.ts          ✅ CSS class + getByRole
│   ├── user-blog.spec.ts    ✅ CSS class + getByRole
│   ├── feed.spec.ts         ✅ CSS class + getByRole
│   └── trending.spec.ts     ✅ CSS class + getByRole
└── playwright.config.ts
```

---

## 6. 구현 체크리스트

### 6.1 Auth Fixture ✅

- [x] `ensureValidPage` 함수 구현
- [x] `mockLogin` - addInitScript 방식
- [x] `mockLogout` - URL 검증 후 localStorage.clear()

### 6.2 Test Files ✅

- [x] comment.spec.ts - CSS class + getByRole 방식
- [x] like.spec.ts - CSS class + getByRole 방식
- [x] follow.spec.ts - CSS class + getByRole 방식
- [x] my-page.spec.ts - CSS class + getByRole 방식
- [x] series.spec.ts - CSS class + getByRole 방식
- [x] tag.spec.ts - CSS class + getByRole 방식
- [x] user-blog.spec.ts - CSS class + getByRole 방식
- [x] feed.spec.ts - CSS class + getByRole 방식
- [x] trending.spec.ts - CSS class + getByRole 방식

### 6.3 data-testid 제거 ✅

- [x] 테스트 파일에서 data-testid 참조 제거
- [x] CSS class + Playwright semantic selector로 대체

---

## 7. 예상 결과

| Metric | Before | After (Target) |
|--------|--------|----------------|
| data-testid in tests | 46개 | 0개 ✅ |
| CSS/Semantic selectors | 0개 | 668개 ✅ |
| Test Pass Rate | ~30% | >= 90% (백엔드 연동 시) |

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-02-03 | Initial design (data-testid based) | Claude |
| 2.0 | 2026-02-03 | Updated component mapping | Claude |
| 3.0 | 2026-02-03 | Changed to Semantic Selector approach (no data-testid) | Claude |
