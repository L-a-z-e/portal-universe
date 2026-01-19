# Components Guide

## 컴포넌트 구조

```
src/components/
├── PostCard.vue          # 게시글 카드 (목록용)
└── HelloWorld.vue        # 데모 컴포넌트

src/views/
├── PostListPage.vue      # 게시글 목록 페이지
├── PostDetailPage.vue    # 게시글 상세 페이지
├── PostWritePage.vue     # 게시글 작성 페이지
└── PostEditPage.vue      # 게시글 수정 페이지
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
