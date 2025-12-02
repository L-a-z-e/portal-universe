<script setup lang="ts">
import { useAuthStore } from "portal_shell/authStore";
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { useRouter } from "vue-router";
import { getPublishedPosts } from "../api/posts";
import type { PostSummaryResponse } from "../dto/post";
import type { PageResponse } from "@/types";
import { Button, Card, SearchBar } from '@portal/design-system';
import PostCard from '../components/PostCard.vue';
import { useSearchStore } from '../stores/searchStore';

const router = useRouter();
const authStore = useAuthStore();
const searchStore = useSearchStore();

// 일반 목록 상태
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

// 검색 모드 여부
const isSearchMode = computed(() => searchStore.keyword.trim().length > 0);

// 현재 표시할 게시글 목록
const displayPosts = computed(() => {
  return isSearchMode.value ? searchStore.results : posts.value;
});

// 현재 로딩 상태
const currentLoading = computed(() => {
  return isSearchMode.value ? searchStore.isSearching : isLoading.value;
});

// 현재 에러 상태
const currentError = computed(() => {
  return isSearchMode.value ? searchStore.error : error.value;
});

// 현재 hasMore 상태
const currentHasMore = computed(() => {
  return isSearchMode.value ? searchStore.hasMore : hasMore.value;
});

// 빈 상태 확인
const isEmpty = computed(() => !currentLoading.value && displayPosts.value.length === 0);

// 더 로드 가능 여부
const canLoadMore = computed(() => currentHasMore.value && !isLoadingMore.value && !currentLoading.value);

// 총 게시글 수
const totalCount = computed(() => {
  return isSearchMode.value ? searchStore.results.length : totalElements.value;
});

// 일반 게시글 목록 로드
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
      posts.value = [...posts.value, ...response.content];
    } else {
      posts.value = response.content;
    }

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
  if (!canLoadMore.value) return;

  if (isSearchMode.value) {
    searchStore.loadMore();
  } else {
    loadPosts(currentPage.value + 1, true);
  }
}

// 새로고침
function refresh() {
  if (isSearchMode.value) {
    searchStore.search(searchStore.keyword);
  } else {
    currentPage.value = 0;
    posts.value = [];
    hasMore.value = true;
    loadPosts(0, false);
  }
}

// 검색 실행
function handleSearch(keyword: string) {
  searchStore.search(keyword);
}

// 검색 초기화
function handleClearSearch() {
  searchStore.clear();
  // 일반 목록이 비어있으면 다시 로드
  if (posts.value.length === 0) {
    loadPosts(0, false);
  }
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
        if (target && target.isIntersecting && canLoadMore.value) {
          loadMore();
        }
      },
      {
        root: null,
        rootMargin: '100px',
        threshold: 0.1
      }
  );

  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value);
  }
}

// 초기화
onMounted(async () => {
  await loadPosts(0, false);
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
  <div class="w-full mx-auto px-4 sm:px-6 py-8">
    <!-- Header -->
    <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">
          📝 Blog
        </h1>
        <p class="text-text-meta">
          {{ isSearchMode ? `"${searchStore.keyword}" 검색 결과` : `총 ${totalCount}개의 게시글` }}
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

    <!-- SearchBar -->
    <div class="mb-8">
      <SearchBar
          v-model="searchStore.keyword"
          placeholder="제목, 내용, 태그로 검색..."
          :loading="searchStore.isSearching"
          @search="handleSearch"
          @clear="handleClearSearch"
      />
    </div>

    <!-- Loading State (초기 로드) -->
    <Card v-if="isInitialLoad && isLoading" class="text-center py-24 bg-bg-muted border-0 shadow-none">
      <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-5"></div>
      <p class="text-text-meta text-lg">게시글을 불러오는 중...</p>
    </Card>

    <!-- Error State -->
    <Card v-else-if="currentError && isEmpty" class="bg-status-error-bg border-status-error/20 py-16 text-center">
      <div class="text-4xl text-status-error mb-4">❌</div>
      <div class="text-status-error font-semibold text-lg mb-2">{{ currentError }}</div>
      <Button variant="secondary" class="mt-4" @click="refresh">
        다시 시도
      </Button>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="isEmpty" class="text-center py-20">
      <div class="text-6xl mb-4">{{ isSearchMode ? '🔍' : '📭' }}</div>
      <h3 class="text-2xl font-bold text-text-heading mb-2">
        {{ isSearchMode ? '검색 결과가 없습니다' : '아직 게시글이 없습니다' }}
      </h3>
      <p class="text-text-meta mb-6">
        {{ isSearchMode ? '다른 검색어를 시도해보세요.' : '첫 게시글을 작성해보세요!' }}
      </p>
      <Button
          v-if="!isSearchMode && authStore.isAuthenticated"
          variant="primary"
          @click="router.push('/write')"
      >
        첫 글 작성하기
      </Button>
    </Card>

    <!-- Post Grid -->
    <div v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6 lg:gap-8">
        <PostCard
            v-for="post in displayPosts"
            :key="post.id"
            :post="post"
            @click="goToPost"
        />
      </div>

      <!-- Infinite Scroll Trigger -->
      <div
          v-if="currentHasMore"
          ref="loadMoreTrigger"
          class="min-h-[100px] flex items-center justify-center"
      >
        <div v-if="isLoadingMore || searchStore.isSearching" class="text-center py-8">
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
          <span class="text-text-meta text-sm font-medium">
            {{ isSearchMode ? '모든 검색 결과를 불러왔습니다' : '모든 게시글을 불러왔습니다' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>