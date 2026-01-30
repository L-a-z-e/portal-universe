# Components Guide

## 컴포넌트 구조

```
src/components/
├── PostCard.vue          # 게시글 카드 (목록용)
├── PostNavigation.vue    # 이전/다음 게시글 네비게이션
├── RelatedPosts.vue      # 관련 게시글
├── MyPostList.vue        # 내 게시글 목록 (상태별 필터)
├── CommentList.vue       # 댓글 목록 (트리 구조)
├── CommentForm.vue       # 댓글/답글/수정 입력 폼
├── CommentItem.vue       # 개별 댓글 (재귀)
├── LikeButton.vue        # 좋아요 토글 (Optimistic UI)
├── SeriesCard.vue        # 시리즈 카드 (목록용)
├── SeriesBox.vue         # 시리즈 네비게이션 (상세 페이지용)
├── FollowButton.vue      # 팔로우/언팔로우 토글
├── FollowerModal.vue     # 팔로워/팔로잉 목록 모달
├── UserProfileCard.vue   # 사용자 프로필 카드
├── ProfileEditForm.vue   # 프로필 수정 폼
├── MySeriesList.vue      # 내 시리즈 목록 관리 (CRUD)
├── LikersModal.vue       # 좋아요 사용자 목록 모달
├── TagAutocomplete.vue   # 태그 자동완성 입력
└── HelloWorld.vue        # 데모 컴포넌트

src/views/
├── PostListPage.vue      # 게시글 목록 (무한 스크롤)
├── PostDetailPage.vue    # 게시글 상세 (Markdown 렌더링)
├── PostWritePage.vue     # 게시글 작성 (Toast UI Editor)
├── PostEditPage.vue      # 게시글 수정
├── TagListPage.vue       # 태그 목록 (검색/정렬/클라우드)
├── TagDetailPage.vue     # 태그별 게시글 (무한 스크롤)
├── SeriesDetailPage.vue  # 시리즈 상세 (순번 목록)
├── MyPage.vue            # 마이페이지 (프로필/게시글/시리즈)
└── UserBlogPage.vue      # 사용자 블로그 (공개 프로필)
```

## 주요 컴포넌트

### 1. PostCard.vue

**역할**: 게시글 요약을 카드 형태로 표시

#### Props

```typescript
interface Props {
  post: PostSummaryResponse;  // 게시글 데이터
}
```

#### Events

```typescript
emit('click', postId: string);  // 카드 클릭 시 발생
```

#### 구조

```vue
<template>
  <Card hoverable @click="handleClick" class="velog-card">
    <!-- 썸네일 영역 (200px 높이) -->
    <div class="thumbnail-wrapper">
      <img :src="thumbnailSrc" :alt="post.title" @error="onImgError" />
    </div>
    
    <!-- 콘텐츠 영역 -->
    <div class="content-wrapper">
      <!-- 제목 (2줄 제한) -->
      <h2 class="post-title">{{ post.title }}</h2>
      
      <!-- 요약 (3줄 제한) -->
      <p class="post-summary">{{ summary }}</p>
      
      <!-- 메타 정보 (태그, 작성자, 통계) -->
      <div class="meta-section">
        <div class="tags-wrapper">
          <Tag v-for="tag in post.tags.slice(0, 3)">{{ tag }}</Tag>
          <Tag v-if="post.tags.length > 3">+{{ post.tags.length - 3 }}</Tag>
        </div>
        
        <div class="author-stats-wrapper">
          <div class="author-info">
            <Avatar :name="post.authorName" />
            <span>{{ post.authorName }}</span>
            <span>{{ relativeTime }}</span>
          </div>
          
          <div class="stats-wrapper">
            <span class="stat-item">👁️ {{ post.viewCount }}</span>
            <span class="stat-item">❤️ {{ post.likeCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </Card>
</template>
```

#### 주요 기능

**1. 썸네일 이미지 에러 핸들링**
```typescript
const imgError = ref(false);
const thumbnailSrc = computed(() => {
  if (imgError.value) {
    return DEFAULT_THUMBNAILS.write;  // 기본 이미지
  }
  return post.thumbnailUrl || DEFAULT_THUMBNAILS[category];
});
```

**2. 상대 시간 계산**
```typescript
const relativeTime = computed(() => {
  const now = new Date();
  const published = new Date(post.publishedAt);
  const diff = now.getTime() - published.getTime();
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  return formattedDate.value;
});
```

**3. 요약 텍스트 추출**
```typescript
const summary = computed(() => {
  if (post.summary) {
    return post.summary.length > 150
      ? post.summary.slice(0, 150) + '...'
      : post.summary;
  }
  // content에서 HTML 제거하고 추출
  const clean = post.content?.replace(/<[^>]*>/g, '') || '';
  return clean.length > 150 ? clean.substring(0, 150) + '...' : clean;
});
```

#### 스타일 특징

- **반응형**: 태블릿 이상에서 제목/요약 크기 증가
- **Velog 스타일**: 썸네일 hover 시 scale(1.05)
- **Scoped CSS**: 다른 컴포넌트와 스타일 격리
- **Design Token**: `--color-*` 변수 사용

#### 사용 예

```vue
<script setup lang="ts">
import PostCard from '@/components/PostCard.vue';
import type { PostSummaryResponse } from '@/dto/post';

const posts = ref<PostSummaryResponse[]>([]);

function goToPost(postId: string) {
  router.push(`/${postId}`);
}
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
    <PostCard
      v-for="post in posts"
      :key="post.id"
      :post="post"
      @click="goToPost"
    />
  </div>
</template>
```

### 2. PostListPage.vue

**역할**: 발행된 게시글 목록을 무한 스크롤로 표시

#### 상태

```typescript
// 일반 목록 상태
const posts = ref<PostSummaryResponse[]>([]);
const currentPage = ref(0);
const pageSize = ref(10);
const hasMore = ref(true);

// 검색 모드
const searchStore = useSearchStore();
const isSearchMode = computed(() => searchStore.keyword.trim().length > 0);

// 표시할 게시글 (검색 vs 일반 목록)
const displayPosts = computed(() => {
  return isSearchMode.value ? searchStore.results : posts.value;
});
```

#### 주요 기능

**1. 게시글 로드**
```typescript
async function loadPosts(page: number = 0, append: boolean = false) {
  try {
    isLoading.value = true;
    const response = await getPublishedPosts(page, pageSize.value);
    
    if (append) {
      posts.value = [...posts.value, ...response.content];
    } else {
      posts.value = response.content;
    }
    
    hasMore.value = !response.last;
  } catch (err) {
    error.value = '게시글을 불러올 수 없습니다';
  }
}
```

**2. 무한 스크롤 구현**
```typescript
function setupIntersectionObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const target = entries[0];
      if (target?.isIntersecting && canLoadMore.value) {
        loadMore();
      }
    },
    {
      root: null,
      rootMargin: '100px',  // 미리 로드
      threshold: 0.1
    }
  );
  
  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value);
  }
}
```

**3. 검색 통합**
```typescript
function handleSearch(keyword: string) {
  searchStore.search(keyword);  // Pinia store에서 검색
}

function handleClearSearch() {
  searchStore.clear();
  // 일반 목록 재로드
}
```

#### 상태 표시

| 상태 | UI |
|------|----|
| 초기 로딩 | 스피너 + "게시글을 불러오는 중..." |
| 에러 | "❌ 게시글 목록을 불러올 수 없습니다" |
| 빈 상태 (검색) | "🔍 검색 결과가 없습니다" |
| 빈 상태 (일반) | "📭 아직 게시글이 없습니다" |
| 게시글 표시 | 반응형 그리드 |
| 더 로드 중 | 스피너 + "더 많은 게시글을 불러오는 중..." |
| 모두 로드 완료 | "✓ 모든 게시글을 불러왔습니다" |

#### 반응형 그리드

```vue
<div class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
  <!-- 열 개수 -->
  <!-- sm (640px): 1열 -->
  <!-- md (768px): 2열 -->
  <!-- lg (1024px): 3열 -->
  <!-- xl (1280px): 4열 -->
  <!-- 2xl (1536px): 5열 -->
</div>
```

### 3. PostDetailPage.vue

**역할**: 게시글 상세 정보와 댓글을 표시

#### 주요 기능

**1. Markdown 렌더링 (Toast UI Editor)**
```typescript
let viewerInstance: Viewer | null = null;

function initViewer(content: string) {
  if (viewerInstance) {
    viewerInstance.destroy();  // 기존 인스턴스 제거
  }
  
  viewerInstance = new Viewer({
    el: viewerElement.value,
    markdown: content,
    plugins: [
      codeSyntaxHighlight  // 코드 신택스 하이라이팅
    ]
  });
}
```

**2. 다크 모드 지원**
```typescript
function detectTheme() {
  const theme = document.documentElement.getAttribute('data-theme');
  isDarkMode.value = theme === 'dark';
  
  if (viewerInstance && viewerElement.value) {
    if (isDarkMode.value) {
      viewerElement.value.classList.add('toastui-editor-dark');
    } else {
      viewerElement.value.classList.remove('toastui-editor-dark');
    }
  }
}
```

**3. 댓글 관리**
```typescript
async function loadComments() {
  try {
    comments.value = await getCommentsByPostId(route.params.postId);
  } catch (err) {
    console.error('Failed to load comments', err);
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return;
  
  try {
    const created = await createComment(route.params.postId, {
      content: newComment.value
    });
    comments.value.push(created);
    newComment.value = '';
  } catch (err) {
    console.error('Failed to create comment', err);
  }
}
```

### 4. PostWritePage.vue & PostEditPage.vue

**역할**: Markdown 에디터를 사용한 게시글 작성/수정

#### 주요 라이브러리

- **@toast-ui/editor**: Markdown 에디터
- **@toast-ui/editor-plugin-code-syntax-highlight**: 코드 하이라이팅
- **prismjs**: Syntax Highlighting

#### 구조

```typescript
import Editor from '@toast-ui/editor';

let editorInstance: Editor | null = null;

function initEditor() {
  editorInstance = new Editor({
    el: editorElement.value,
    height: '600px',
    initialValue: post?.content || '',
    previewStyle: 'vertical',  // 세로 분할
    plugins: [codeSyntaxHighlight]
  });
}

function getContent(): string {
  return editorInstance?.getMarkdown() || '';
}
```

#### 폼 필드

```typescript
const form = ref({
  title: '',              // 제목
  summary: '',            // 요약
  content: '',            // Markdown 내용
  category: '',           // 카테고리
  tags: [] as string[],   // 태그 배열
  thumbnailUrl: '',       // 썸네일 이미지 URL
  status: 'DRAFT'         // DRAFT, PUBLISHED, DELETED
});
```

### 5. CommentList.vue

**역할**: 게시글의 댓글을 트리 구조(루트 댓글 + 대댓글)로 표시 및 관리

#### Props

```typescript
interface Props {
  postId: string
  currentUserId?: string
}
```

#### 주요 기능

- 댓글 CRUD (생성, 수정, 삭제, 대댓글)
- 트리 구조 관리 (루트 댓글과 대댓글 분리)
- CommentItem + CommentForm 하위 컴포넌트 조합
- API: `getCommentsByPostId`, `createComment`, `updateComment`, `deleteComment`

### 6. CommentForm.vue

**역할**: 댓글/답글/수정의 3가지 모드를 지원하는 입력 폼

#### Props

```typescript
interface Props {
  postId: string
  parentCommentId?: string | null
  initialContent?: string
  mode?: 'create' | 'edit' | 'reply'
  placeholder?: string
}
```

#### Events

```typescript
emit('submit', content: string)  // 제출
emit('cancel')                   // 취소
```

#### 주요 기능

- 모드별 버튼 텍스트 변경 ('등록' / '수정')
- 공백 검사 및 제출 검증
- 취소 버튼 (create 모드에서는 숨김)

### 7. CommentItem.vue

**역할**: 개별 댓글을 재귀적으로 표시 (대댓글 지원)

#### Props

```typescript
interface Props {
  comment: CommentResponse
  depth: number
  replies?: CommentResponse[]
  currentUserId?: string
}
```

#### Events

```typescript
emit('reply', parentCommentId: string)
emit('edit', commentId: string, content: string)
emit('delete', commentId: string)
emit('submitReply', commentId: string, content: string)
emit('toggleReplies', commentId: string)
```

#### 주요 기능

- 본인 댓글만 수정/삭제 버튼 표시
- 삭제된 댓글 표시 ("삭제된 댓글입니다")
- 답글 폼 토글
- CommentItem 재귀 렌더링 (depth + 1)
- 상대 시간 표시 (`formatRelativeTime`)

### 8. LikeButton.vue

**역할**: 좋아요 토글 버튼 (Optimistic UI 적용)

#### Props

```typescript
interface Props {
  postId: string
  initialLiked?: boolean
  initialCount?: number
}
```

#### Events

```typescript
emit('likeChanged', liked: boolean, count: number)
```

#### 주요 기능

- Optimistic UI: 즉시 UI 업데이트 후 API 호출, 실패 시 롤백
- Heart 아이콘 애니메이션 (heartBeat)
- 초기 상태 API 조회 (`getLikeStatus`)
- API: `toggleLike`, `getLikeStatus`

### 9. SeriesCard.vue

**역할**: 시리즈 요약을 카드 형태로 표시 (목록용)

#### Props

```typescript
interface Props {
  series: SeriesListResponse
}
```

#### Events

```typescript
emit('click', seriesId: string)
```

#### 주요 기능

- 시리즈 기본 정보 표시 (이름, 설명, 작성자)
- 게시글 개수 + 마지막 업데이트 날짜
- 썸네일 이미지 에러 핸들링
- 호버 효과

### 10. SeriesBox.vue

**역할**: 게시글 상세 페이지에서 시리즈 네비게이션 표시

#### Props

```typescript
interface Props {
  seriesId: string
  currentPostId: string
}
```

#### 주요 기능

- 현재 게시글의 시리즈 정보 및 위치 표시 (n/total)
- 이전/다음 게시글 네비게이션 버튼
- 시리즈 전체 목록 보기 링크
- API: `getSeriesById`, `getSeriesPosts`

### 11. FollowButton.vue

**역할**: 팔로우/언팔로우 토글 버튼

#### Props

```typescript
interface Props {
  username: string
  targetUuid: string
  initialFollowing?: boolean
  size?: 'sm' | 'md' | 'lg'
  showText?: boolean
}
```

#### Events

```typescript
emit('followChanged', following: boolean, followerCount: number, followingCount: number)
```

#### 주요 기능

- Optimistic UI 업데이트
- 호버 시 "취소" 텍스트 표시
- 에러 처리 (401 미인증, 400 자기 자신 등)
- `useFollowStore` 연동

### 12. FollowerModal.vue

**역할**: 팔로워/팔로잉 사용자 목록을 모달로 표시

#### Props

```typescript
interface Props {
  username: string
  isOpen: boolean
  type: 'followers' | 'following'
}
```

#### Events

```typescript
emit('close')
```

#### 주요 기능

- 팔로워/팔로잉 리스트 표시 (닉네임, 유저명, bio)
- 페이지네이션 (더 보기 버튼)
- 각 사용자에 FollowButton 표시
- 사용자 클릭 시 프로필 페이지 이동

### 13. UserProfileCard.vue

**역할**: 사용자 프로필 정보를 카드 형태로 표시

#### Props

```typescript
interface Props {
  user: UserProfileResponse
  isCurrentUser?: boolean
}
```

#### Events

```typescript
emit('followChanged', followerCount: number, followingCount: number)
```

#### 주요 기능

- 아바타, 이름, bio, 웹사이트, 가입일 표시
- 팔로워/팔로잉 통계 (클릭 시 FollowerModal)
- 현재 사용자가 아닐 경우 FollowButton 표시
- 웹사이트 프로토콜 자동 추가

### 14. ProfileEditForm.vue

**역할**: 프로필 정보 수정 폼

#### Props

```typescript
interface Props {
  user: UserProfileResponse
}
```

#### Events

```typescript
emit('success', user: UserProfileResponse)
emit('cancel')
```

#### 주요 기능

- 이름, Username, bio(200자 제한), 웹사이트 수정
- Username 최초 1회 설정 (이후 변경 불가)
- Username 중복 확인 (디바운스)
- Username 유효성 검증 (3-20자, 영문/숫자/_/-)
- API: `updateProfile`, `setUsername`, `checkUsername`

### 15. PostNavigation.vue

**역할**: 이전/다음 게시글 네비게이션

#### Props

```typescript
interface Props {
  postId: string
  scope?: 'all' | 'author' | 'category' | 'series'
}
```

#### 주요 기능

- 범위 선택 지원 (전체/작성자/카테고리/시리즈)
- 썸네일 + 제목으로 이전/다음 표시
- 반응형: 모바일 1열, 태블릿 이상 2열
- API: `getPostNavigation`

### 16. RelatedPosts.vue

**역할**: 관련 게시글 표시 (PostCard 그리드)

#### Props

```typescript
interface Props {
  postId: string
  tags?: string[]
  limit?: number  // 기본값: 4
}
```

#### 주요 기능

- 관련 게시글 조회 및 PostCard 그리드 표시
- 반응형: 1열 → 2열 → 4열
- API: `getRelatedPosts`

### 17. MyPostList.vue

**역할**: 내 게시글 목록 (상태별 필터 + 관리)

#### 주요 기능

- 상태 필터 탭: ALL / PUBLISHED / DRAFT
- 게시글 관리: 수정, 삭제, 발행(Draft→Published)
- 메타정보 표시 (날짜, 조회수, 좋아요)
- 페이지네이션 (더 보기)
- API: `getMyPosts`, `deletePost`, `changePostStatus`

### 18. TagListPage.vue (View)

**역할**: 전체 태그 목록 페이지 (검색/정렬/클라우드 뷰)

#### 주요 기능

- 태그 검색 (태그명, 설명)
- 정렬 옵션: 인기순 / 이름순 / 최신순
- 뷰 모드: 그리드(인기순/최신순) / 태그 클라우드(이름순)
- 태그 크기 계산 (postCount 기반)
- 통계 요약 표시
- API: `getAllTags`

### 19. TagDetailPage.vue (View)

**역할**: 특정 태그의 게시글 목록 (무한 스크롤)

#### Props

```typescript
interface Props {
  tagName: string
}
```

#### 주요 기능

- 태그 정보 + 해당 게시글 표시
- IntersectionObserver 기반 무한 스크롤
- 태그 색상 (해시 기반)
- API: `getTagByName`, `getPostsByTag`

### 20. SeriesDetailPage.vue (View)

**역할**: 시리즈 상세 페이지 (순번 목록)

#### 주요 기능

- 시리즈 정보 카드 (썸네일, 설명, 작성자)
- 게시글을 순번과 함께 리스트로 표시
- 메타정보 (날짜, 조회수, 좋아요)
- API: `getSeriesById`, `getSeriesPosts`

### 21. MyPage.vue (View)

**역할**: 마이페이지 (프로필 + 콘텐츠 관리)

#### 주요 기능

- 내 프로필 조회 + 수정 모드 토글
- 탭 네비게이션: 내 게시글 / 내 시리즈
- 하위 컴포넌트: UserProfileCard, ProfileEditForm, MyPostList
- API: `getMyProfile`

### 22. UserBlogPage.vue (View)

**역할**: 다른 사용자의 블로그 페이지

#### Props

```typescript
interface Props {
  username: string
}
```

#### 주요 기능

- 사용자 공개 프로필 + 게시글 표시
- 무한 스크롤 (스크롤 이벤트)
- Username 변경 감시 및 리로드
- 하위 컴포넌트: UserProfileCard, PostCard
- API: `getPublicProfile`, `getPostsByAuthor`

### 23. MySeriesList.vue

**역할**: 내 시리즈 목록 관리 (CRUD + 게시글 관리)

#### Props

None (자체 데이터 로드)

#### Events

None (내부에서 라우팅 처리)

#### 상태

```typescript
const seriesList = ref<SeriesListResponse[]>([])
const showModal = ref(false)              // 생성/수정 모달
const showDeleteConfirm = ref(false)      // 삭제 확인 모달
const showPostsModal = ref(false)         // 게시글 관리 모달
const modalMode = ref<'create' | 'edit'>('create')
const formData = ref({
  name: '',
  description: '',
  thumbnailUrl: ''
})
```

#### 주요 기능

- **시리즈 CRUD**: 생성/수정/삭제 모달 기반 관리
- **게시글 관리 모달**: 시리즈에 포함된 게시글 목록 + 추가 가능한 게시글 목록 표시, 게시글 추가/제거
- 시리즈 카드 그리드 표시 (auto-fill, minmax 300px)
- 시리즈별 게시글 수 + 최종 업데이트 날짜 표시
- 카드 클릭 시 시리즈 상세 페이지 이동
- API: `getMySeries`, `createSeries`, `updateSeries`, `deleteSeries`, `getSeriesPosts`, `addPostToSeries`, `removePostFromSeries`, `getMyPosts`
- Design System 컴포넌트: Button, Card, Input, Textarea, Modal

### 24. LikersModal.vue

**역할**: 게시글 좋아요 사용자 목록 모달

#### Props

```typescript
interface Props {
  postId: string;       // 게시글 ID
  isOpen: boolean;      // 모달 열림 여부
}
```

#### Events

```typescript
emit('close')  // 모달 닫힘
```

#### 주요 기능

- `isOpen` watch를 통해 모달 열릴 때 자동으로 데이터 로드 (페이지 초기화)
- 좋아요를 누른 사용자 목록 표시 (Avatar + username + 좋아요 날짜)
- 페이지네이션 지원 (더 보기 버튼, 페이지당 20건)
- 사용자 클릭 시 `/@{username}` 프로필 페이지 이동
- 빈 상태 / 로딩 상태 UI 처리
- API: `getLikers`
- Design System 컴포넌트: Modal, Avatar, Button, Spinner

### 25. TagAutocomplete.vue

**역할**: 태그 자동완성 입력 (v-model 지원)

#### Props

```typescript
interface Props {
  modelValue: string[];  // v-model 바인딩, 선택된 태그 배열
}
```

#### Events

```typescript
emit('update:modelValue', tags: string[])  // 태그 목록 업데이트
```

#### 주요 기능

- 태그 입력 시 자동완성 드롭다운 제안 (API 연동)
- Debounce 검색 (300ms) - 입력이 1자 이상일 때 검색 실행
- 이미 선택된 태그는 자동완성 목록에서 제외
- Enter 키로 태그 추가 / Escape 키로 드롭다운 닫기
- 추가 버튼 클릭으로도 태그 추가 가능
- 선택된 태그는 closable Tag 컴포넌트로 표시 (삭제 가능)
- 중복 태그 방지
- blur 시 드롭다운 자동 닫힘 (200ms 딜레이로 클릭 이벤트 보장)
- API: `searchTags`
- Design System 컴포넌트: Input, Tag

## Design System 컴포넌트 사용

### Button

```vue
<script setup>
import { Button } from '@portal/design-system';
</script>

<template>
  <!-- 기본 버튼 -->
  <Button @click="handleClick">클릭</Button>
  
  <!-- 색상 -->
  <Button variant="primary">주요 버튼</Button>
  <Button variant="secondary">보조 버튼</Button>
  <Button variant="danger">위험 버튼</Button>
  
  <!-- 크기 -->
  <Button size="sm">작음</Button>
  <Button size="md">중간</Button>
  <Button size="lg">큼</Button>
</template>
```

### Card

```vue
<template>
  <!-- 기본 카드 -->
  <Card>콘텐츠</Card>
  
  <!-- 호버 효과 -->
  <Card hoverable @click="handleClick">클릭 가능</Card>
  
  <!-- 패딩 제거 -->
  <Card padding="none">이미지</Card>
</template>
```

### Tag

```vue
<template>
  <Tag variant="default" size="sm">태그</Tag>
  <Tag variant="primary">주요 태그</Tag>
</template>
```

### Avatar

```vue
<template>
  <Avatar :name="authorName" size="xs" />  <!-- 초소 -->
  <Avatar :name="authorName" size="md" />  <!-- 중간 -->
</template>
```

### SearchBar

```vue
<template>
  <SearchBar
    v-model="keyword"
    placeholder="검색..."
    :loading="isSearching"
    @search="handleSearch"
    @clear="handleClear"
  />
</template>
```

## 컴포넌트 베스트 프랙티스

### 1. Props 검증

```typescript
interface Props {
  post: PostSummaryResponse;  // 필수
  compact?: boolean;          // 선택사항
}

const props = withDefaults(defineProps<Props>(), {
  compact: false
});
```

### 2. 이벤트 발생

```typescript
const emit = defineEmits<{
  click: [postId: string];
  delete: [postId: string];
}>();

function handleClick() {
  emit('click', post.id);
}
```

### 3. Lifecycle 관리

```typescript
onMounted(() => {
  // 초기화
  loadData();
  setupObserver();
});

onBeforeUnmount(() => {
  // 정리
  observer?.disconnect();
  editor?.destroy();
});
```

### 4. 에러 처리

```typescript
try {
  await loadPost();
} catch (err) {
  error.value = '게시글을 불러올 수 없습니다';
  console.error(err);
}
```

## 관련 문서

- [README.md](./README.md) - 모듈 개요
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처
- [API.md](./API.md) - API 사용법
