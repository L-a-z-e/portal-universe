# API-UI Gap Implementation

## 개요

blog-service에는 API가 존재하지만 blog-frontend에 UI가 구현되지 않았던 기능들을 식별하고 구현한 내용을 정리한 문서입니다.

총 9개 항목의 기능을 새롭게 구현하거나 기존 페이지에 추가하여 백엔드 API와 프론트엔드 UI 간의 갭을 해소했습니다.

## 구현 항목

### 1. 게시글 삭제 버튼

**파일**: `src/views/PostDetailPage.vue`

**변경 내용**:
- 작성자에게만 표시되는 삭제 버튼 추가
- 삭제 확인 모달 구현
- 삭제 성공 시 메인 페이지로 리다이렉트

**사용 API**: `deletePost(postId)`

**동작 흐름**:
```
1. 게시글 상세 페이지에서 작성자 확인 (currentUser.id === post.author.id)
2. 작성자인 경우 "삭제" 버튼 표시
3. 삭제 버튼 클릭 시 확인 모달 표시
4. 확인 시 deletePost API 호출
5. 성공 시 "게시글이 삭제되었습니다" 메시지와 함께 홈으로 리다이렉트
```

**구현 포인트**:
- `v-if="isAuthor"` 조건부 렌더링으로 작성자 체크
- `@portal/design-system-vue`의 Modal, Button 컴포넌트 활용
- 에러 처리 및 사용자 피드백 제공

---

### 2. 태그 자동완성

**새 파일**: `src/components/TagAutocomplete.vue`
**수정 파일**: `src/views/PostWritePage.vue`, `src/views/PostEditPage.vue`

**사용 API**: `searchTags(query)`

**동작 흐름**:
```
1. 태그 입력 필드에 텍스트 입력
2. 300ms debounce 후 searchTags API 호출
3. 검색 결과를 드롭다운으로 표시
4. Enter 키 또는 항목 클릭으로 태그 추가
5. 선택된 태그는 Tag 컴포넌트로 표시
```

**구현 포인트**:
- Composition API의 `watchDebounced` 활용
- 키보드 네비게이션 지원 (ArrowUp, ArrowDown, Enter)
- 중복 태그 추가 방지
- 드롭다운 외부 클릭 시 자동 닫힘
- 로딩 상태 표시

**컴포넌트 Props**:
```typescript
interface Props {
  modelValue: string[]  // v-model로 양방향 바인딩
  maxTags?: number      // 최대 태그 개수 (기본값: 10)
}
```

---

### 3. 좋아요 사용자 목록 모달

**새 파일**: `src/components/LikersModal.vue`
**수정 파일**: `src/views/PostDetailPage.vue`

**사용 API**: `getLikers(postId, page, size)`

**동작 흐름**:
```
1. 게시글 상세 페이지에서 좋아요 수 클릭
2. LikersModal 열림
3. getLikers API로 좋아요 누른 사용자 목록 조회
4. 페이지네이션으로 더 많은 사용자 로드
5. 사용자 클릭 시 해당 프로필 페이지로 이동
```

**구현 포인트**:
- 페이지네이션 지원 (page 0부터 시작, size 20)
- Avatar 컴포넌트로 프로필 이미지 표시
- 사용자 이름 클릭 시 `/users/:userId` 라우트로 이동
- 로딩 상태 및 빈 상태 처리
- "더 보기" 버튼으로 추가 로드

**모달 구조**:
```
[Modal Header] 좋아요 (N명)
[User List]
  - Avatar + Username (클릭 가능)
  - Avatar + Username
  - ...
[Load More Button] (다음 페이지 있을 경우)
```

---

### 4. MyPage 시리즈 관리

**새 파일**: `src/components/MySeriesList.vue`
**수정 파일**: `src/views/MyPage.vue`

**사용 API**:
- `getMySeries()` - 내 시리즈 목록 조회
- `createSeries(data)` - 새 시리즈 생성
- `updateSeries(seriesId, data)` - 시리즈 수정
- `deleteSeries(seriesId)` - 시리즈 삭제

**동작 흐름**:
```
1. MyPage에서 "시리즈" 탭 클릭
2. MySeriesList 컴포넌트 렌더링
3. getMySeries로 시리즈 목록 로드
4. "새 시리즈" 버튼 클릭 시 생성 모달
5. 시리즈 카드에서 "수정" / "삭제" 버튼
6. 수정 시 편집 모달, 삭제 시 확인 모달
```

**구현 포인트**:
- 그리드 레이아웃 (반응형: 1열 → 2열 → 3열)
- 시리즈 카드에 제목, 설명, 포스트 수 표시
- 생성/수정 모달에서 제목(필수), 설명(선택) 입력
- 삭제 시 "이 시리즈에 포함된 포스트도 삭제됩니까?" 안내
- 낙관적 업데이트 (Optimistic UI) 적용

**시리즈 카드 구조**:
```
[Card]
  [Title] 시리즈 제목
  [Description] 시리즈 설명 (최대 2줄)
  [Meta] 포스트 N개
  [Actions] 수정 | 삭제
```

---

### 5. 카테고리별 게시물 페이지

**새 파일**: `src/views/CategoryListPage.vue`
**라우터**: `/categories` (쿼리 파라미터: `?category=카테고리명`)

**사용 API**:
- `getCategoryStats()` - 카테고리별 통계
- `getPostsByCategory(category, page, size)` - 카테고리별 게시물
- `getPublishedPosts(page, size)` - 전체 게시물 (카테고리 미선택 시)

**동작 흐름**:
```
1. /categories 접근 시 전체 게시물 표시
2. 카테고리 선택 시 URL 쿼리 업데이트 (?category=카테고리명)
3. 해당 카테고리 게시물 필터링
4. 무한 스크롤로 추가 게시물 로드
5. 카테고리 선택 해제 시 전체 게시물로 복귀
```

**구현 포인트**:
- **반응형 레이아웃**:
  - 데스크탑 (lg 이상): 사이드바 + 게시물 리스트
  - 모바일: 카테고리 그리드 + 게시물 리스트 (세로 스크롤)
- **URL 쿼리 동기화**: 브라우저 뒤로가기 지원
- **IntersectionObserver**로 무한 스크롤 구현
- 카테고리별 게시물 수 표시
- 빈 상태 처리 ("해당 카테고리에 게시물이 없습니다")

**레이아웃 구조** (데스크탑):
```
[Page Container]
  [Sidebar - 고정]
    - 전체 보기
    - 카테고리 A (N개)
    - 카테고리 B (N개)
    - ...
  [Main Content - 스크롤]
    - Post Card
    - Post Card
    - ...
    - [IntersectionObserver Target]
```

---

### 6. 고급 검색 UI

**새 파일**: `src/views/AdvancedSearchPage.vue`
**라우터**: `/search/advanced`

**사용 API**: `searchPostsAdvanced(request)`

**Request 구조**:
```typescript
interface AdvancedSearchRequest {
  keyword?: string          // 키워드 검색
  category?: string         // 카테고리 필터
  tags?: string[]           // 태그 필터 (AND 조건)
  authorId?: string         // 작성자 필터
  startDate?: string        // 시작 날짜 (ISO 8601)
  endDate?: string          // 종료 날짜 (ISO 8601)
  page: number
  size: number
}
```

**동작 흐름**:
```
1. 검색 조건 입력 폼 표시
2. "검색" 버튼 클릭 시 searchPostsAdvanced API 호출
3. 검색 결과를 리스트로 표시
4. 무한 스크롤로 추가 결과 로드
5. 검색 조건 변경 시 결과 초기화 후 재검색
```

**구현 포인트**:
- 여러 필터 조합 가능 (AND 조건)
- 날짜 선택기 (Date Input) 지원
- 태그는 TagAutocomplete 컴포넌트 재사용
- 검색 결과 없을 경우 빈 상태 표시
- 검색 조건 초기화 버튼
- URL 쿼리로 검색 조건 유지 (북마크 가능)

**폼 필드**:
```
[키워드] _______________
[카테고리] _____________
[태그] [태그1] [태그2] + 자동완성
[작성자 ID] ____________
[작성일]
  시작: [날짜] ~ 종료: [날짜]
[검색] [초기화]
```

---

### 7. 블로그 통계 대시보드

**새 파일**: `src/views/StatsPage.vue`
**라우터**: `/stats`

**사용 API**:
- `getBlogStats()` - 블로그 전체 통계
- `getCategoryStats()` - 카테고리별 통계
- `getPopularTags(limit)` - 인기 태그
- `getAuthorStats(authorId)` - 작성자 통계 (로그인 시)

**동작 흐름**:
```
1. /stats 접근 시 전체 통계 로드
2. 로그인 상태면 내 통계도 함께 표시
3. 카테고리 통계 테이블 렌더링
4. 인기 태그를 태그 클라우드로 시각화
```

**구현 포인트**:
- **4개 통계 카드** (그리드 레이아웃):
  - 총 게시물 수
  - 총 조회수
  - 총 좋아요 수
  - 총 댓글 수
- **내 통계 섹션** (로그인 시):
  - 내 게시물 수
  - 내 게시물 조회수
  - 받은 좋아요 수
  - 받은 댓글 수
- **카테고리 통계 테이블**:
  - 카테고리명 | 게시물 수 | 조회수
  - 게시물 수 기준 정렬
- **인기 태그 클라우드**:
  - 사용 횟수에 따른 폰트 크기 차별화
  - 클릭 시 해당 태그 검색 페이지로 이동

**페이지 구조**:
```
[전체 통계 - 4개 카드 그리드]
  [Card] 게시물: 1,234
  [Card] 조회수: 56,789
  [Card] 좋아요: 2,345
  [Card] 댓글: 3,456

[내 통계] (로그인 시)
  [Card] 내 게시물: 42
  [Card] 내 조회수: 1,234
  [Card] 받은 좋아요: 89
  [Card] 받은 댓글: 123

[카테고리 통계]
  [Table]
    카테고리 | 게시물 | 조회수
    ---------|--------|--------
    Tech     | 100    | 5,000
    Life     | 50     | 2,000

[인기 태그]
  [Tag Cloud]
    Vue React TypeScript ...
```

---

### 8. 라우터 업데이트

**파일**: `src/router/index.ts`

**변경 내용**: 3개 신규 라우트 추가

```typescript
{
  path: '/categories',
  name: 'CategoryList',
  component: () => import('@/views/CategoryListPage.vue'),
  meta: { title: '카테고리' }
},
{
  path: '/search/advanced',
  name: 'AdvancedSearch',
  component: () => import('@/views/AdvancedSearchPage.vue'),
  meta: { title: '고급 검색' }
},
{
  path: '/stats',
  name: 'Stats',
  component: () => import('@/views/StatsPage.vue'),
  meta: { title: '통계' }
}
```

**구현 포인트**:
- Lazy loading으로 초기 번들 크기 최적화
- meta 필드에 페이지 제목 정의 (향후 SEO 활용)
- 기존 라우트와 충돌 없음 확인

---

### 9. 시리즈 포스트 순서 변경 / 추가/제거 (부분 구현)

**현재 상태**:
- ✅ 시리즈 CRUD (생성, 조회, 수정, 삭제) - MySeriesList에서 완료
- ❌ 시리즈 내 포스트 순서 변경 UI (드래그 앤 드롭) - 미구현
- ❌ 시리즈에 포스트 추가/제거 UI - 미구현

**사용 가능한 API**:
- `reorderSeriesPosts(seriesId, postIds)` - 포스트 순서 변경
- `addPostToSeries(seriesId, postId)` - 포스트 추가
- `removePostFromSeries(seriesId, postId)` - 포스트 제거

**향후 구현 필요**:
- 시리즈 상세 페이지에서 포스트 리스트를 드래그 앤 드롭으로 순서 변경
- 게시글 작성/수정 시 시리즈 선택 드롭다운
- 시리즈 상세 페이지에서 "포스트 추가" 버튼

---

## 기술 스택

### Core
- **Vue 3**: Composition API (`<script setup lang="ts">`)
- **TypeScript**: 타입 안전성 보장
- **Vue Router**: SPA 라우팅

### UI Components
- **@portal/design-system-vue**: 프로젝트 공통 디자인 시스템
  - Button, Card, Modal, Input, Textarea
  - Tag, Avatar, Spinner
  - 일관된 스타일링 및 접근성 보장

### State Management
- **Pinia**: `useAuthStore` from `portal/stores`
  - 인증 상태 관리
  - 현재 사용자 정보 접근

### Styling
- **CSS Variables**: 디자인 토큰 기반 스타일링
- **Tailwind CSS**: 유틸리티 퍼스트 CSS 프레임워크
- **Responsive Design**: 모바일 우선 반응형 레이아웃

### 브라우저 API
- **IntersectionObserver**: 무한 스크롤 구현
- **History API**: URL 쿼리 동기화 (pushState, replaceState)

---

## 변경 파일 요약

| 파일 | 변경 유형 | 구현 항목 | 설명 |
|------|----------|----------|------|
| `src/views/PostDetailPage.vue` | 수정 | #1, #3 | 삭제 버튼 추가, 좋아요 목록 모달 연동 |
| `src/views/PostWritePage.vue` | 수정 | #2 | 태그 자동완성 컴포넌트 적용 |
| `src/views/PostEditPage.vue` | 수정 | #2 | 태그 자동완성 컴포넌트 적용 |
| `src/views/MyPage.vue` | 수정 | #4 | 시리즈 탭 활성화, MySeriesList 통합 |
| `src/components/TagAutocomplete.vue` | **신규** | #2 | 태그 자동완성 컴포넌트 |
| `src/components/LikersModal.vue` | **신규** | #3 | 좋아요 사용자 목록 모달 |
| `src/components/MySeriesList.vue` | **신규** | #4 | 시리즈 CRUD 관리 UI |
| `src/views/CategoryListPage.vue` | **신규** | #5 | 카테고리별 게시물 페이지 |
| `src/views/AdvancedSearchPage.vue` | **신규** | #6 | 고급 검색 페이지 |
| `src/views/StatsPage.vue` | **신규** | #7 | 블로그 통계 대시보드 |
| `src/router/index.ts` | 수정 | #8 | 3개 라우트 등록 |

**파일 변경 통계**:
- 신규 생성: 6개
- 수정: 5개
- 총: 11개 파일

---

## 남은 작업 (향후 개선)

### 1. 시리즈 포스트 관리 UI
- [ ] 시리즈 상세 페이지에서 드래그 앤 드롭으로 포스트 순서 변경
- [ ] `reorderSeriesPosts` API 연동
- [ ] 라이브러리: `@vueuse/draggable` 또는 `vue-draggable-next` 고려

### 2. 시리즈에 포스트 추가/제거
- [ ] 게시글 작성/수정 페이지에서 시리즈 선택 드롭다운
- [ ] 시리즈 상세 페이지에서 "포스트 추가" 버튼
- [ ] `addPostToSeries`, `removePostFromSeries` API 연동

### 3. 카테고리 선택 개선
- [ ] 고급 검색의 카테고리 필드를 드롭다운으로 변경 (현재 텍스트 입력)
- [ ] 카테고리 목록 API 호출하여 자동완성 지원

### 4. 통계 페이지 고도화
- [ ] 차트 라이브러리 도입 (Chart.js, ECharts 등)
- [ ] 시계열 통계 (일별/주별/월별 조회수 추이)
- [ ] 인기 게시물 Top 10 섹션

### 5. 검색 결과 정렬 옵션
- [ ] 고급 검색에 정렬 기준 추가 (최신순, 인기순, 조회수순)
- [ ] 카테고리 페이지에도 정렬 옵션 추가

### 6. 접근성 개선
- [ ] 키보드 네비게이션 강화 (Tab, Enter, Esc)
- [ ] ARIA 속성 추가 (role, aria-label 등)
- [ ] 스크린 리더 테스트

### 7. 성능 최적화
- [ ] 무한 스크롤 가상화 (Virtual Scrolling)
- [ ] 이미지 지연 로딩 (Lazy Loading)
- [ ] API 응답 캐싱 (Pinia 스토어 활용)

---

## 참고 문서

- [API Documentation](./API.md) - blog-service API 명세
- [Components](./COMPONENTS.md) - Design System 컴포넌트 가이드
- [Architecture](./ARCHITECTURE.md) - 프론트엔드 아키텍처
- [Status](./STATUS.md) - 프로젝트 진행 상황

---

**작성일**: 2026-01-28
**작성자**: Laze
**버전**: 1.0
