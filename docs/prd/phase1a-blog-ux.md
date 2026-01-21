# Phase 1-A: Blog UX Enhancement PRD

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0 |
| 작성일 | 2026-01-21 |
| 대상 서비스 | blog-service, blog-frontend |
| Phase | 1-A (auth-service 영향 없음) |
| 벤치마크 | Velog |

---

## 1. 개요 및 목표

### 1.1 배경

Portal Universe 블로그는 기본적인 CRUD 기능을 갖추고 있으나, 사용자 참여를 유도하고 콘텐츠 탐색을 개선할 UX 요소가 부족합니다. Velog와 같은 현대적 블로그 플랫폼의 UX 패턴을 벤치마킹하여 사용자 경험을 향상시킵니다.

### 1.2 목표

- **사용자 참여도 향상**: 좋아요, 댓글을 통한 상호작용 강화
- **콘텐츠 탐색 개선**: 시리즈, 태그, 관련 게시글, 트렌딩 콘텐츠
- **독자 유지율 증가**: 이전/다음 포스트 네비게이션, 시리즈 구조
- **정보 구조 개선**: 대댓글 트리 구조, 접기/펼치기

### 1.3 범위

**포함 (Phase 1-A)**
- 좋아요 시스템 (blog-service + blog-frontend)
- 이전/다음 포스트 네비게이션 API (blog-service)
- 시리즈 API 클라이언트 및 UI (blog-frontend)
- 상세 페이지 개선 (blog-frontend)
- 태그 페이지 (blog-frontend)
- 메인 페이지 탭 개선 (blog-frontend)
- 대댓글 UI 개선 (blog-frontend)

**제외 (향후 Phase)**
- 사용자 팔로우/팔로워 시스템
- 북마크/저장 기능
- 알림 시스템
- 검색 자동완성

---

## 2. 기능별 상세 요구사항

### 2.1 좋아요 시스템

#### 사용자 스토리
```
As a 독자
I want to 게시글에 좋아요를 누를 수 있다
So that 마음에 드는 콘텐츠에 긍정적 피드백을 줄 수 있다
```

#### 상세 기능

**Backend (blog-service)**

1. **Like Entity 생성**
   - Collection: `likes`
   - Fields:
     - `id` (String): MongoDB ObjectId
     - `postId` (String, Indexed): 게시글 ID
     - `userId` (String, Indexed): 사용자 ID
     - `createdAt` (LocalDateTime): 생성일시
   - Unique Index: `{postId, userId}`

2. **Like API 엔드포인트**
   ```
   POST   /api/v1/blog/posts/{postId}/like      - 좋아요 추가
   DELETE /api/v1/blog/posts/{postId}/like      - 좋아요 취소
   GET    /api/v1/blog/posts/{postId}/like      - 좋아요 여부 확인 (현재 사용자)
   ```

3. **비즈니스 로직**
   - 좋아요 추가 시: `Post.likeCount++`, Like 문서 생성
   - 좋아요 취소 시: `Post.likeCount--`, Like 문서 삭제
   - 중복 좋아요 방지: Unique Index + 409 Conflict 응답
   - 인증 필요: JWT 토큰 검증

4. **Error Codes**
   ```java
   B020: POST_NOT_FOUND
   B021: ALREADY_LIKED
   B022: LIKE_NOT_FOUND
   ```

**Frontend (blog-frontend)**

1. **좋아요 버튼 컴포넌트** (`LikeButton.vue`)
   - Props: `postId`, `initialLikeCount`, `initialIsLiked`
   - State: `isLiked`, `likeCount`, `isLoading`
   - 로그인 필요 시 로그인 페이지로 리다이렉트
   - 애니메이션: 하트 아이콘 + 숫자 증가/감소 효과
   - Optimistic UI: 즉시 UI 업데이트, 실패 시 롤백

2. **통합 위치**
   - 게시글 상세 페이지 하단 (댓글 위)
   - 게시글 카드 하단 (PostCard 컴포넌트)

#### UI/UX 요구사항

**버튼 상태**
- 비활성 (기본): 빈 하트 아이콘 + 회색
- 활성 (좋아요): 채워진 하트 아이콘 + 빨강
- 로딩: Spinner + 비활성화

**애니메이션**
- 좋아요 시: 하트 아이콘 scale up → down (0.2초)
- 숫자 변경: Fade transition (0.15초)

**레이아웃 (Velog 스타일)**
```
┌─────────────────────────────────────┐
│  ❤️ 128        💬 15        📤 공유  │
└─────────────────────────────────────┘
```

#### 수용 기준 (Acceptance Criteria)

- [ ] 로그인한 사용자는 게시글에 좋아요를 추가/취소할 수 있다
- [ ] 좋아요 수가 실시간으로 업데이트된다
- [ ] 중복 좋아요 시 에러 메시지 표시 없이 정상 처리된다 (멱등성)
- [ ] 비로그인 사용자는 좋아요 버튼 클릭 시 로그인 페이지로 이동한다
- [ ] 네트워크 실패 시 사용자에게 알림 후 이전 상태로 롤백된다

---

### 2.2 이전/다음 포스트 네비게이션

#### 사용자 스토리
```
As a 독자
I want to 현재 게시글의 이전/다음 글로 쉽게 이동할 수 있다
So that 연속적으로 콘텐츠를 탐색할 수 있다
```

#### 상세 기능

**Backend (blog-service)**

1. **Navigation API**
   ```
   GET /api/v1/blog/posts/{postId}/navigation
   ```

2. **Response DTO**
   ```json
   {
     "previous": {
       "id": "...",
       "title": "이전 게시글 제목",
       "publishedAt": "2026-01-20T10:00:00",
       "thumbnailUrl": "..."
     },
     "next": {
       "id": "...",
       "title": "다음 게시글 제목",
       "publishedAt": "2026-01-21T14:00:00",
       "thumbnailUrl": "..."
     }
   }
   ```

3. **정렬 기준**
   - 기본: `publishedAt DESC` (최신순)
   - 같은 작성자의 글 우선 고려 (Optional, 설정 가능)
   - 같은 카테고리 우선 (Optional, 설정 가능)

4. **성능 최적화**
   - Indexed query on `{publishedAt, status}`
   - Projection: id, title, publishedAt, thumbnailUrl만 조회

**Frontend (blog-frontend)**

1. **PostNavigation 컴포넌트**
   - Layout: 좌우 화살표 버튼 + 썸네일 + 제목
   - 없는 경우 숨김 또는 비활성화 표시

#### UI/UX 요구사항 (Velog 스타일)

```
┌───────────────────────────────────────────────┐
│  ← 이전 글                    다음 글 →         │
│  [썸네일]                      [썸네일]        │
│  "Kubernetes 배포 자동화"      "React Hook..."  │
└───────────────────────────────────────────────┘
```

**반응형**
- Desktop: 좌우 배치
- Mobile: 상하 배치, 썸네일 작게

#### 수용 기준

- [ ] 게시글 상세 하단에 이전/다음 글 네비게이션이 표시된다
- [ ] 클릭 시 해당 게시글로 이동한다
- [ ] 첫/마지막 글인 경우 해당 방향 버튼이 숨겨진다
- [ ] 썸네일이 없는 경우 기본 이미지가 표시된다

---

### 2.3 시리즈 API 클라이언트 및 UI

#### 사용자 스토리
```
As a 작성자
I want to 연관된 게시글을 시리즈로 묶을 수 있다
So that 독자가 순차적으로 학습할 수 있다
```

```
As a 독자
I want to 시리즈 목록을 보고 순서대로 읽을 수 있다
So that 주제에 대해 체계적으로 학습할 수 있다
```

#### 상세 기능

**Backend (blog-service)**

기존 Series API 활용:
```
GET    /api/v1/blog/series                    - 시리즈 목록 (페이징)
GET    /api/v1/blog/series/{seriesId}         - 시리즈 상세 (게시글 목록 포함)
POST   /api/v1/blog/series                    - 시리즈 생성 (작성자)
PUT    /api/v1/blog/series/{seriesId}         - 시리즈 수정
DELETE /api/v1/blog/series/{seriesId}         - 시리즈 삭제
PUT    /api/v1/blog/series/{seriesId}/order   - 게시글 순서 변경
```

**Frontend (blog-frontend)**

1. **시리즈 페이지** (`/series`)
   - 시리즈 카드 그리드
   - 카드 정보: 제목, 설명, 게시글 수, 작성자, 썸네일
   - 검색/필터: 작성자, 최신순/인기순

2. **시리즈 상세 페이지** (`/series/:seriesId`)
   - 시리즈 헤더: 제목, 설명, 작성자, 총 게시글 수
   - 게시글 목록: 순서, 제목, 발행일, 읽음 표시
   - 진행률 표시 (읽은 글 / 전체 글)

3. **시리즈 관리 페이지** (`/series/manage`)
   - 내 시리즈 목록
   - 생성/수정/삭제
   - 게시글 추가/제거/순서 변경 (Drag & Drop)

#### UI/UX 요구사항 (Velog 스타일)

**시리즈 카드**
```
┌─────────────────────────────┐
│ [썸네일]                     │
│ Spring Boot 완벽 가이드      │
│ Spring Boot의 A to Z         │
│                              │
│ @author · 12개 글            │
└─────────────────────────────┘
```

**시리즈 상세 - 게시글 목록**
```
┌─────────────────────────────────────────┐
│ 1. Spring Boot 시작하기        [✓ 읽음] │
│ 2. Spring Boot 설정 파일                │
│ 3. Spring Data JPA 기초                 │
└─────────────────────────────────────────┘
```

#### 수용 기준

- [ ] 시리즈 목록 페이지에서 모든 시리즈를 볼 수 있다
- [ ] 시리즈 상세 페이지에서 게시글 목록을 순서대로 볼 수 있다
- [ ] 작성자는 시리즈를 생성/수정/삭제할 수 있다
- [ ] 작성자는 게시글 순서를 Drag & Drop으로 변경할 수 있다
- [ ] 게시글 상세 페이지에서 시리즈 정보가 표시된다 (해당 시리즈가 있는 경우)

---

### 2.4 상세 페이지 개선

#### 사용자 스토리
```
As a 독자
I want to 게시글을 읽으면서 관련 정보를 쉽게 탐색할 수 있다
So that 더 많은 유용한 콘텐츠를 발견할 수 있다
```

#### 상세 기능

**추가 요소**

1. **시리즈 박스** (게시글이 시리즈에 속한 경우)
   - 위치: 게시글 상단 또는 사이드바
   - 내용: 시리즈 제목, 현재 위치 (N/M), 이전/다음 글 링크
   - 접기/펼치기 가능

2. **관련 게시글** (추천 알고리즘)
   - 위치: 게시글 하단
   - 기준: 같은 태그, 같은 카테고리, 같은 작성자
   - 최대 4개 표시 (카드 형태)

3. **목차 (Table of Contents)**
   - 위치: 사이드바 (Desktop), 접기/펼치기 버튼 (Mobile)
   - 내용: H2, H3 제목 추출
   - 현재 읽는 섹션 하이라이트
   - 클릭 시 스크롤 이동

4. **태그 목록**
   - 위치: 게시글 하단 (제목 아래 또는 본문 아래)
   - 클릭 시 태그 페이지로 이동

#### UI/UX 요구사항 (Velog 스타일)

**시리즈 박스 (펼침 상태)**
```
┌─────────────────────────────────────┐
│ 📚 Spring Boot 완벽 가이드           │
│ ─────────────────────────────────   │
│ 1. Spring Boot 시작하기              │
│ 2. Spring Boot 설정 파일 ◀ 현재 위치│
│ 3. Spring Data JPA 기초              │
│                                      │
│ [이전 글]           [다음 글]        │
└─────────────────────────────────────┘
```

**관련 게시글**
```
이런 글도 있어요

┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│[썸네일]│ │[썸네일]│ │[썸네일]│ │[썸네일]│
│제목    │ │제목    │ │제목    │ │제목    │
│@작성자 │ │@작성자 │ │@작성자 │ │@작성자 │
└──────┘ └──────┘ └──────┘ └──────┘
```

**레이아웃 (Desktop)**
```
┌─────────────────────────────────┬─────────┐
│ [시리즈 박스]                    │  목차   │
│                                  │  ─────  │
│ 제목                             │  1. ... │
│ 태그: #react #hooks              │  2. ... │
│                                  │  3. ... │
│ ────────────────────────         │         │
│                                  │         │
│ 본문 내용                         │         │
│                                  │         │
│ ────────────────────────         │         │
│                                  │         │
│ ❤️ 좋아요  💬 댓글                │         │
│                                  │         │
│ [이전/다음 네비게이션]            │         │
│                                  │         │
│ [관련 게시글]                     │         │
│                                  │         │
│ [댓글 섹션]                       │         │
└─────────────────────────────────┴─────────┘
```

#### 수용 기준

- [ ] 시리즈에 속한 게시글은 시리즈 박스가 표시된다
- [ ] 게시글 하단에 관련 게시글 4개가 추천된다
- [ ] Desktop에서 목차가 사이드바에 표시되며 스크롤에 따라 현재 위치가 하이라이트된다
- [ ] Mobile에서 목차를 접기/펼치기할 수 있다
- [ ] 태그 클릭 시 해당 태그 페이지로 이동한다

---

### 2.5 태그 페이지

#### 사용자 스토리
```
As a 독자
I want to 특정 태그로 필터링된 게시글을 볼 수 있다
So that 관심 주제의 콘텐츠를 모아서 볼 수 있다
```

#### 상세 기능

**Backend (blog-service)**

기존 API 활용:
```
GET /api/v1/blog/posts?tag={tagName}&page=0&size=20
```

추가 API (Optional):
```
GET /api/v1/blog/tags                  - 태그 목록 (빈도순)
GET /api/v1/blog/tags/{tagName}/stats  - 태그 통계 (게시글 수)
```

**Frontend (blog-frontend)**

1. **태그 페이지** (`/tags/:tagName`)
   - 헤더: 태그 이름, 게시글 수
   - 게시글 목록: 최신순/인기순 정렬
   - 페이징

2. **태그 탐색 페이지** (`/tags`)
   - 전체 태그 목록 (태그 클라우드 또는 그리드)
   - 태그별 게시글 수 표시
   - 검색/필터

#### UI/UX 요구사항 (Velog 스타일)

**태그 페이지 헤더**
```
┌─────────────────────────────────────┐
│ #react                               │
│ 128개의 포스트                        │
│                                      │
│ [최신순 ▼]  [인기순]                 │
└─────────────────────────────────────┘
```

**태그 클라우드 (탐색 페이지)**
```
#react (45)  #vue (32)  #typescript (28)
#javascript (98)  #css (23)  #nodejs (41)
...
```

#### 수용 기준

- [ ] `/tags/:tagName` 경로로 태그 페이지에 접근할 수 있다
- [ ] 태그 페이지에서 해당 태그가 달린 모든 게시글을 볼 수 있다
- [ ] 최신순/인기순으로 정렬할 수 있다
- [ ] 태그 탐색 페이지에서 모든 태그와 게시글 수를 볼 수 있다
- [ ] 태그 클릭 시 해당 태그 페이지로 이동한다

---

### 2.6 메인 페이지 탭 개선

#### 사용자 스토리
```
As a 독자
I want to 트렌딩 콘텐츠와 최신 콘텐츠를 따로 볼 수 있다
So that 내 관심사에 맞는 콘텐츠를 효율적으로 탐색할 수 있다
```

#### 상세 기능

**Backend (blog-service)**

1. **트렌딩 API**
   ```
   GET /api/v1/blog/posts/trending?period={today|week|month}&page=0&size=20
   ```

2. **트렌딩 계산 로직**
   - Score = `(likeCount * 3) + (viewCount * 1) + (commentCount * 5)`
   - Time decay: 최근 게시글 가중치 부여
   - 기간 필터: 오늘, 이번 주, 이번 달

3. **최신 게시글 API** (기존)
   ```
   GET /api/v1/blog/posts?page=0&size=20&sort=publishedAt,desc
   ```

**Frontend (blog-frontend)**

1. **메인 페이지 탭**
   - 탭: [트렌딩] [최신]
   - 트렌딩 탭: 기간 필터 (오늘, 이번 주, 이번 달)
   - 최신 탭: 정렬 필터 (카테고리 전체/특정)

2. **상태 관리**
   - 탭 전환 시 이전 스크롤 위치 유지 (Optional)
   - URL 쿼리 파라미터로 탭/필터 상태 유지

#### UI/UX 요구사항 (Velog 스타일)

```
┌─────────────────────────────────────┐
│ [트렌딩]  [최신]                     │
│                                      │
│ 기간: [오늘] [이번 주] [이번 달]     │
│                                      │
│ [게시글 카드]                        │
│ [게시글 카드]                        │
│ [게시글 카드]                        │
│ ...                                  │
└─────────────────────────────────────┘
```

#### 수용 기준

- [ ] 메인 페이지에 [트렌딩] [최신] 탭이 표시된다
- [ ] 트렌딩 탭에서 기간 필터를 선택할 수 있다
- [ ] 최신 탭에서 카테고리 필터를 선택할 수 있다 (Optional)
- [ ] 탭 전환 시 URL 쿼리 파라미터가 업데이트된다
- [ ] 브라우저 뒤로가기 시 이전 탭/필터 상태가 복원된다

---

### 2.7 대댓글 UI 개선

#### 사용자 스토리
```
As a 독자
I want to 댓글에 대한 답글을 작성하고 트리 구조로 볼 수 있다
So that 댓글 간의 맥락을 이해하고 대화에 참여할 수 있다
```

#### 상세 기능

**Backend (blog-service)**

기존 Comment Entity 활용:
- `parentCommentId` 필드 사용
- 기존 API로 대댓글 생성 가능

**Frontend (blog-frontend)**

1. **댓글 트리 구조 렌더링**
   - 최대 깊이: 3단계 (댓글 → 대댓글 → 대대댓글)
   - Indent로 깊이 표시
   - 부모-자식 관계 시각화 (왼쪽 라인)

2. **답글 작성 UI**
   - 각 댓글에 [답글] 버튼
   - 클릭 시 해당 댓글 아래 입력 폼 표시
   - `@username` 자동 멘션 (Optional)

3. **접기/펼치기**
   - 대댓글이 많은 경우 (3개 이상) 일부만 표시
   - [N개의 답글 보기] 버튼으로 펼침
   - 각 댓글에 접기 버튼 (깊이 1 이상)

4. **성능 최적화**
   - 가상 스크롤 (댓글이 많은 경우)
   - Lazy load: 대댓글 on-demand 로드 (Optional)

#### UI/UX 요구사항 (Velog 스타일)

```
┌─────────────────────────────────────────┐
│ 💬 댓글 15                               │
│                                          │
│ @user1 · 2시간 전                        │
│ 좋은 글 감사합니다!                       │
│ [답글] [수정] [삭제]                     │
│                                          │
│   │ @user2 · 1시간 전                    │
│   │ 동의합니다!                          │
│   │ [답글] [수정] [삭제]                 │
│   │                                      │
│   │   │ @user3 · 30분 전                 │
│   │   │ 저도요!                          │
│   │   │ [수정] [삭제]                    │
│   │                                      │
│   │ [2개의 답글 더 보기]                 │
│                                          │
│ @user4 · 5시간 전                        │
│ 질문이 있습니다...                        │
│ [답글] [수정] [삭제]                     │
└─────────────────────────────────────────┘
```

**답글 작성 폼**
```
┌─────────────────────────────────────────┐
│ @user1 · 2시간 전                        │
│ 좋은 글 감사합니다!                       │
│ [답글] [수정] [삭제]                     │
│                                          │
│ ┌─────────────────────────────────────┐ │
│ │ @user1에게 답글 작성                 │ │
│ │ [텍스트 입력 영역]                   │ │
│ │                                      │ │
│ │ [취소] [작성]                        │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

#### 수용 기준

- [ ] 댓글이 트리 구조로 표시된다 (최대 3단계)
- [ ] 각 댓글에 [답글] 버튼이 있다
- [ ] [답글] 클릭 시 해당 댓글 아래 입력 폼이 표시된다
- [ ] 대댓글이 3개 이상인 경우 일부만 표시하고 [N개의 답글 보기] 버튼이 나타난다
- [ ] 대댓글을 접기/펼치기할 수 있다
- [ ] 왼쪽 라인으로 부모-자식 관계가 시각화된다

---

## 3. 기술 요구사항

### 3.1 Backend (blog-service)

#### 기술 스택
- Java 17, Spring Boot 3.5.5
- MongoDB (Document Store)
- Spring Security + JWT

#### 새로운 Entity/Collection

| Entity | Collection | Key Fields |
|--------|-----------|-----------|
| Like | `likes` | `id`, `postId`, `userId`, `createdAt` |

#### 새로운 API 엔드포인트

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/blog/posts/{postId}/like` | 좋아요 추가 |
| DELETE | `/api/v1/blog/posts/{postId}/like` | 좋아요 취소 |
| GET | `/api/v1/blog/posts/{postId}/like` | 좋아요 여부 확인 |
| GET | `/api/v1/blog/posts/{postId}/navigation` | 이전/다음 포스트 |
| GET | `/api/v1/blog/posts/trending` | 트렌딩 포스트 |
| GET | `/api/v1/blog/tags` | 태그 목록 |
| GET | `/api/v1/blog/tags/{tagName}/stats` | 태그 통계 |

#### Error Codes 추가

```java
// BlogErrorCode.java
B020("B020", "게시글을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
B021("B021", "이미 좋아요한 게시글입니다", HttpStatus.CONFLICT),
B022("B022", "좋아요를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
```

#### 인덱스 전략

```javascript
// likes collection
db.likes.createIndex({ postId: 1, userId: 1 }, { unique: true })
db.likes.createIndex({ userId: 1, createdAt: -1 })

// posts collection (기존 + 추가)
db.posts.createIndex({ tags: 1, publishedAt: -1 })
db.posts.createIndex({ status: 1, publishedAt: -1 })
db.posts.createIndex({ likeCount: -1, publishedAt: -1 })
```

### 3.2 Frontend (blog-frontend)

#### 기술 스택
- Vue 3 (Composition API)
- TypeScript
- Pinia (State Management)
- Vite
- Tailwind CSS
- @portal/design-system-vue

#### 새로운 컴포넌트

| 컴포넌트 | 경로 | 용도 |
|---------|------|------|
| `LikeButton.vue` | `components/` | 좋아요 버튼 |
| `PostNavigation.vue` | `components/` | 이전/다음 포스트 |
| `SeriesBox.vue` | `components/` | 시리즈 박스 |
| `RelatedPosts.vue` | `components/` | 관련 게시글 |
| `TableOfContents.vue` | `components/` | 목차 |
| `CommentTree.vue` | `components/` | 댓글 트리 |
| `CommentReplyForm.vue` | `components/` | 답글 작성 폼 |

#### 새로운 페이지

| 페이지 | 경로 | 용도 |
|--------|------|------|
| `TagPage.vue` | `/tags/:tagName` | 태그별 게시글 목록 |
| `TagExplore.vue` | `/tags` | 전체 태그 탐색 |
| `SeriesListPage.vue` | `/series` | 시리즈 목록 |
| `SeriesDetailPage.vue` | `/series/:seriesId` | 시리즈 상세 |
| `SeriesManagePage.vue` | `/series/manage` | 시리즈 관리 (작성자) |

#### API Client 추가

```typescript
// api/likes.ts
export const toggleLike = (postId: string): Promise<ApiResponse<LikeResponse>>
export const checkLikeStatus = (postId: string): Promise<ApiResponse<boolean>>

// api/posts.ts
export const getPostNavigation = (postId: string): Promise<ApiResponse<NavigationResponse>>
export const getTrendingPosts = (period: string, page: number): Promise<ApiResponse<Page<PostSummary>>>
export const getRelatedPosts = (postId: string): Promise<ApiResponse<PostSummary[]>>

// api/series.ts
export const getSeriesList = (page: number): Promise<ApiResponse<Page<SeriesListResponse>>>
export const getSeriesDetail = (seriesId: string): Promise<ApiResponse<SeriesResponse>>
export const createSeries = (request: SeriesCreateRequest): Promise<ApiResponse<SeriesResponse>>
export const updateSeries = (seriesId: string, request: SeriesUpdateRequest): Promise<ApiResponse<SeriesResponse>>
export const deleteSeries = (seriesId: string): Promise<ApiResponse<void>>
export const updateSeriesOrder = (seriesId: string, request: SeriesPostOrderRequest): Promise<ApiResponse<void>>

// api/tags.ts
export const getTagList = (): Promise<ApiResponse<TagStats[]>>
export const getPostsByTag = (tagName: string, page: number): Promise<ApiResponse<Page<PostSummary>>>
```

#### 상태 관리 (Pinia)

```typescript
// stores/likeStore.ts
export const useLikeStore = defineStore('like', () => {
  const likedPosts = ref<Set<string>>(new Set())

  const toggleLike = async (postId: string) => { ... }
  const isLiked = (postId: string) => likedPosts.value.has(postId)

  return { likedPosts, toggleLike, isLiked }
})

// stores/postStore.ts
export const usePostStore = defineStore('post', () => {
  const trendingPosts = ref<PostSummary[]>([])
  const trendingPeriod = ref<'today' | 'week' | 'month'>('week')

  const fetchTrendingPosts = async () => { ... }

  return { trendingPosts, trendingPeriod, fetchTrendingPosts }
})
```

### 3.3 성능 요구사항

| 지표 | 목표 |
|------|------|
| 좋아요 API 응답 시간 | < 200ms (p95) |
| 트렌딩 포스트 조회 | < 500ms (p95) |
| 댓글 트리 렌더링 | < 100ms (100개 댓글 기준) |
| 페이지 전환 시간 | < 300ms |
| Time to Interactive | < 3s (초기 로드) |

### 3.4 접근성 (a11y)

- [ ] 좋아요 버튼: `aria-label` 제공 ("좋아요 추가" / "좋아요 취소")
- [ ] 댓글 트리: 적절한 ARIA role (`tree`, `treeitem`)
- [ ] 키보드 탐색: Tab, Enter, Escape 지원
- [ ] 색상 대비: WCAG AA 기준 (4.5:1)
- [ ] 스크린 리더: 중요 동작 알림

---

## 4. 제약사항 및 의존성

### 4.1 제약사항

**Phase 1-A 제외 사항**
- auth-service 수정 불가
- 알림 시스템 연동 불가 (Phase 1-B 이후)
- 실시간 업데이트 (WebSocket) 제외
- 검색 자동완성 제외
- 북마크/저장 기능 제외

**기술적 제약**
- MongoDB를 사용하므로 복잡한 조인 불가
- Denormalization 전략 필요 (likeCount, commentCount 등)
- 좋아요 수 정합성: Eventual Consistency 허용

### 4.2 의존성

**Backend**
- common-library (ApiResponse, CustomBusinessException)
- Spring Security (JWT 인증)
- MongoDB indexes (성능)

**Frontend**
- @portal/design-system-vue (Button, Card, Tag, Avatar 등)
- portal-shell (apiClient, authStore)
- Vue Router (동적 라우팅)

### 4.3 마이그레이션 고려사항

**기존 데이터**
- Post 컬렉션: `likeCount`, `commentCount` 필드 추가 필요
- 기존 Comment: `parentCommentId` 활용 가능
- 기존 Series: API만 추가, Entity 수정 불필요

**점진적 배포**
1. Backend API 배포 (좋아요, 네비게이션, 트렌딩)
2. Frontend 컴포넌트 배포 (좋아요 버튼)
3. Frontend 페이지 배포 (태그, 시리즈)
4. Frontend 상세 페이지 개선
5. Frontend 댓글 UI 개선

---

## 5. 검증 계획

### 5.1 Unit Tests

**Backend**
- LikeService: 좋아요 추가/취소, 중복 방지, 멱등성
- PostService: 트렌딩 계산, 네비게이션 조회
- CommentService: 트리 구조 조회 (기존)

**Frontend**
- LikeButton: 상태 변경, Optimistic UI, 롤백
- CommentTree: 트리 렌더링, 접기/펼치기
- useLikeStore: 상태 관리 로직

### 5.2 Integration Tests

- 좋아요 E2E: 추가 → 조회 → 취소
- 시리즈 E2E: 생성 → 게시글 추가 → 순서 변경 → 삭제
- 댓글 E2E: 댓글 작성 → 대댓글 작성 → 조회

### 5.3 Manual Tests

| 시나리오 | 기대 결과 |
|---------|----------|
| 비로그인 사용자가 좋아요 클릭 | 로그인 페이지로 리다이렉트 |
| 좋아요 추가 후 새로고침 | 좋아요 상태 유지 |
| 트렌딩 탭에서 기간 변경 | 콘텐츠 목록 업데이트 |
| 댓글 3단계 깊이 작성 | 정상 표시, 4단계 답글 버튼 없음 |
| 시리즈 게시글 순서 변경 (DnD) | 순서 저장 및 반영 |
| Mobile에서 목차 펼치기 | 오버레이 또는 모달로 표시 |

### 5.4 성능 Tests

- Lighthouse: Performance, Accessibility, SEO 점수 측정
- k6: 좋아요 API 동시성 테스트 (1000 req/s)
- 댓글 1000개 렌더링 테스트 (가상 스크롤)

---

## 6. 릴리즈 계획

### 6.1 Phase 1-A Milestones

| 마일스톤 | 기능 | 예상 기간 |
|----------|------|-----------|
| M1 | 좋아요 시스템 (Backend + Frontend) | 3일 |
| M2 | 이전/다음 네비게이션 (Backend + Frontend) | 2일 |
| M3 | 시리즈 API 클라이언트 및 UI | 4일 |
| M4 | 상세 페이지 개선 (시리즈 박스, 관련 게시글, 목차) | 5일 |
| M5 | 태그 페이지 (Backend + Frontend) | 3일 |
| M6 | 메인 페이지 탭 개선 (트렌딩/최신) | 3일 |
| M7 | 대댓글 UI 개선 | 4일 |
| M8 | 테스트 및 버그 수정 | 3일 |

**총 예상 기간**: 27일 (약 5.5주)

### 6.2 배포 전략

**Canary Deployment**
- Frontend: 10% → 50% → 100%
- Backend: Blue/Green (즉시 롤백 가능)

**Feature Flags**
- `FEATURE_LIKE_SYSTEM`: 좋아요 기능 활성화
- `FEATURE_TRENDING_TAB`: 트렌딩 탭 활성화
- `FEATURE_SERIES_UI`: 시리즈 UI 활성화

### 6.3 Rollback Plan

- Frontend: 이전 버전 배포 (Kubernetes Deployment)
- Backend: Blue/Green 전환
- Database: MongoDB 롤백 불필요 (additive changes only)

---

## 7. 성공 지표 (KPI)

| 지표 | 현재 | 목표 (Phase 1-A 완료 후) |
|------|------|--------------------------|
| 평균 세션 시간 | - | +30% |
| 페이지뷰/세션 | - | +50% |
| 좋아요 클릭률 | 0% | 15% (독자 대비) |
| 댓글 작성률 | - | +20% |
| 시리즈 페이지 방문율 | 0% | 10% (전체 방문 대비) |
| 태그 페이지 방문율 | 0% | 8% (전체 방문 대비) |
| 이탈률 | - | -20% |

---

## 8. 참고 자료

### 8.1 Velog 벤치마크

**참고 링크**
- https://velog.io (메인 페이지 레이아웃)
- https://velog.io/@username/post-title (게시글 상세 페이지)
- https://velog.io/tags/react (태그 페이지)
- https://velog.io/@username/series/series-name (시리즈 페이지)

**주요 UX 패턴**
- 미니멀한 디자인
- 읽기에 최적화된 타이포그래피
- 사이드바 목차 (Desktop)
- 시리즈 네비게이션
- 태그 기반 탐색
- 트렌딩/최신 탭

### 8.2 기술 문서

- [MongoDB Indexing Best Practices](https://docs.mongodb.com/manual/indexes/)
- [Vue 3 Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Pinia State Management](https://pinia.vuejs.org/)
- [WCAG 2.1 Accessibility Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

### 8.3 내부 문서

- `docs/architecture/blog-service.md` - 블로그 서비스 아키텍처
- `.claude/rules/vue.md` - Vue 컴포넌트 패턴
- `.claude/rules/spring.md` - Spring Boot 패턴
- `docs/api/blog-api.md` - 블로그 API 명세

---

## 9. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-01-21 | 초기 작성 | - |
