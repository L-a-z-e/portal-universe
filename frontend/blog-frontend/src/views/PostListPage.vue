<script setup lang="ts">
import { useAuthStore } from "portal_shell/authStore";
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { useRouter } from "vue-router";
import { getPublishedPosts } from "../api/posts";
import type { PostSummaryResponse } from "../dto/post";
import type { PageResponse } from "@/types";
import { Button, Card } from '@portal/design-system';
import PostCard from '../components/PostCard.vue';

const router = useRouter();
const authStore = useAuthStore();

// 페이징 상태
const posts = ref<PostSummaryResponse[]>([]);
const currentPage = ref(0);
const pageSize = ref(10);
const totalPages = ref(0);
const totalElements = ref(0);
const hasMore = ref(true);

// 로딩/에러 상태
const isLoading = ref(false);
const isLoadingMore = ref(false);
const error = ref<string | null>(null);

// 초기 로드 여부
const isInitialLoad = ref(true);

// 무한 스크롤 트리거 요소
const loadMoreTrigger = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 계산된 속성
const isEmpty = computed(() => !isLoading.value && posts.value.length === 0);
const canLoadMore = computed(() => hasMore.value && !isLoadingMore.value && !isLoading.value);

// 게시글 목록 로드
async function loadPosts(page: number = 0, append: boolean = false) {
  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
    }

    error.value = null;

    const response: PageResponse<PostSummaryResponse> = await getPublishedPosts(page, pageSize.value);

    if (append) {
      // 기존 목록에 추가 (무한 스크롤)
      posts.value = [...posts.value, ...response.content];
    } else {
      // 새로 로드 (초기 or 새로고침)
      posts.value = response.content;
    }

    // 페이징 메타데이터 업데이트
    currentPage.value = response.number;
    totalPages.value = response.totalPages;
    totalElements.value = response.totalElements;
    hasMore.value = !response.last;

  } catch (err) {
    console.error('Failed to fetch posts:', err);
    error.value = '게시글 목록을 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    isLoading.value = false;
    isLoadingMore.value = false;
    isInitialLoad.value = false;
  }
}

// 다음 페이지 로드
function loadMore() {
  if (canLoadMore.value) {
    loadPosts(currentPage.value + 1, true);
  }
}

// 새로고침
function refresh() {
  currentPage.value = 0;
  posts.value = [];
  hasMore.value = true;
  loadPosts(0, false);
}

// 게시글 클릭
function goToPost(postId: string) {
  router.push(`/${postId}`);
}

// Intersection Observer 설정
function setupIntersectionObserver() {
  if (observer) {
    observer.disconnect();
  }

  observer = new IntersectionObserver(
      (entries) => {
        const target = entries[0];
        // 요소가 화면에 보이고, 더 로드할 수 있으면 자동 로드
        if (target && target.isIntersecting && canLoadMore.value) {
          loadMore();
        }
      },
      {
        root: null, // viewport 기준
        rootMargin: '100px', // 100px 전에 미리 로드
        threshold: 0.1 // 10% 보이면 트리거
      }
  );

  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value);
  }
}

// 초기화
onMounted(async () => {
  // 초기 데이터 로드
  await loadPosts(0, false);

  // Intersection Observer 설정
  setupIntersectionObserver();
});

// 정리
onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect();
    observer = null;
  }
});
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 py-8">
    <!-- Header -->
    <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-10">
      <div>
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">
          📝 Blog
        </h1>
        <p class="text-text-meta">
          {{ totalElements > 0 ? `총 ${totalElements}개의 게시글` : '게시글' }}
        </p>
      </div>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          size="md"
          @click="router.push('/write')"
      >
        ✍️ 새 글 작성
      </Button>
    </header>

    <!-- Loading State (초기 로드) -->
    <Card v-if="isInitialLoad && isLoading" class="text-center py-24 bg-bg-muted border-0 shadow-none">
      <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-5"></div>
      <p class="text-text-meta text-lg">게시글을 불러오는 중...</p>
    </Card>

    <!-- Error State -->
    <Card v-else-if="error && isEmpty" class="bg-status-error-bg border-status-error/20 py-16 text-center">
      <div class="text-4xl text-status-error mb-4">❌</div>
      <div class="text-status-error font-semibold text-lg mb-2">{{ error }}</div>
      <Button variant="secondary" class="mt-4" @click="refresh">
        다시 시도
      </Button>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="isEmpty" class="text-center py-20">
      <div class="text-6xl mb-4">📭</div>
      <h3 class="text-2xl font-bold text-text-heading mb-2">아직 게시글이 없습니다</h3>
      <p class="text-text-meta mb-6">첫 게시글을 작성해보세요!</p>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          @click="router.push('/write')"
      >
        첫 글 작성하기
      </Button>
    </Card>

    <!-- Post Grid -->
    <div v-else>
      <div class="grid gap-6 sm:gap-8 sm:grid-cols-2">
        <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @click="goToPost"
        />
      </div>

      <!-- Infinite Scroll Trigger (보이지 않는 감시 요소) -->
      <div
          v-if="hasMore"
          ref="loadMoreTrigger"
          class="infinite-scroll-trigger"
      >
        <!-- 로딩 인디케이터 -->
        <div v-if="isLoadingMore" class="text-center py-8">
          <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <p class="text-text-meta text-sm">더 많은 게시글을 불러오는 중...</p>
        </div>
      </div>

      <!-- 모두 로드 완료 -->
      <div v-else class="text-center py-8">
        <div class="inline-flex items-center gap-2 px-4 py-2 bg-bg-muted rounded-full">
          <svg class="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
          </svg>
          <span class="text-text-meta text-sm font-medium">모든 게시글을 불러왔습니다</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 반응형 그리드 */
@media (min-width: 768px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* 무한 스크롤 트리거 영역 */
.infinite-scroll-trigger {
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>