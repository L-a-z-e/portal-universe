<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getCategoryStats, getPostsByCategory, getPublishedPosts } from '../api/posts';
import type { CategoryStats, PostSummaryResponse, PageResponse } from '@/types';
import { Spinner, useApiError } from '@portal/design-vue';
import PostCard from '../components/PostCard.vue';

const router = useRouter();
const route = useRoute();
const { getErrorMessage } = useApiError();

// 카테고리 관련 상태
const categories = ref<CategoryStats[]>([]);
const selectedCategory = ref<string | null>(null);
const categoriesLoading = ref(false);
const categoriesError = ref<string | null>(null);

// 게시글 목록 상태
const posts = ref<PostSummaryResponse[]>([]);
const currentPage = ref(1);
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

// 빈 상태 확인
const isEmpty = computed(() => !isLoading.value && posts.value.length === 0);

// 더 로드 가능 여부
const canLoadMore = computed(() => hasMore.value && !isLoadingMore.value && !isLoading.value);

// 총 게시글 수
const totalCount = computed(() => totalElements.value);

// 카테고리 통계 로드
async function loadCategories() {
  try {
    categoriesLoading.value = true;
    categoriesError.value = null;
    categories.value = await getCategoryStats();
  } catch (err) {
    console.error('Failed to fetch category stats:', err);
    categoriesError.value = getErrorMessage(err, '카테고리 목록을 불러올 수 없습니다.');
  } finally {
    categoriesLoading.value = false;
  }
}

// 게시글 목록 로드
async function loadPosts(page: number = 1, append: boolean = false) {
  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
    }

    error.value = null;

    let response: PageResponse<PostSummaryResponse>;

    if (selectedCategory.value) {
      response = await getPostsByCategory(selectedCategory.value, page, pageSize.value);
    } else {
      response = await getPublishedPosts(page, pageSize.value);
    }

    if (append) {
      posts.value = [...posts.value, ...response.items];
    } else {
      posts.value = response.items;
    }

    currentPage.value = response.page;
    totalPages.value = response.totalPages;
    totalElements.value = response.totalElements;
    hasMore.value = response.page < response.totalPages;

  } catch (err) {
    console.error('Failed to fetch posts:', err);
    error.value = getErrorMessage(err, '게시글 목록을 불러올 수 없습니다.');
  } finally {
    isLoading.value = false;
    isLoadingMore.value = false;
    isInitialLoad.value = false;
  }
}

// 다음 페이지 로드
function loadMore() {
  if (!canLoadMore.value) return;
  loadPosts(currentPage.value + 1, true);
}

// 카테고리 선택
function selectCategory(category: string | null) {
  if (selectedCategory.value === category) return;

  selectedCategory.value = category;
  currentPage.value = 1;
  posts.value = [];
  hasMore.value = true;

  updateQueryParams();
  loadPosts(1, false);
}

// URL 쿼리 파라미터 업데이트
function updateQueryParams() {
  const query: Record<string, string> = {};

  if (selectedCategory.value) {
    query.category = selectedCategory.value;
  }

  router.replace({ query });
}

// URL 쿼리 파라미터로부터 초기 상태 설정
function initializeFromQuery() {
  const { category } = route.query;
  if (typeof category === 'string' && category.trim()) {
    selectedCategory.value = category;
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
  initializeFromQuery();
  await loadCategories();
  await loadPosts(1, false);
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
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Header -->
      <header class="mb-8">
        <h1 class="text-2xl font-bold text-text-heading mb-1">카테고리</h1>
        <p class="text-sm text-text-meta">
          {{ selectedCategory ? `${selectedCategory} - ${totalCount}개의 게시글` : `전체 ${totalCount}개의 게시글` }}
        </p>
      </header>

      <!-- 카테고리 에러 -->
      <div v-if="categoriesError" class="mb-6 p-3 bg-status-error-bg border border-status-error/20 rounded-lg text-sm text-status-error">
        {{ categoriesError }}
      </div>

      <!-- 카테고리 필터 -->
      <div v-if="!categoriesLoading" class="flex flex-wrap items-center gap-2 mb-8">
        <button
          @click="selectCategory(null)"
          class="px-3 py-1.5 text-xs font-medium rounded-full transition-colors"
          :class="selectedCategory === null
            ? 'bg-brand-primary text-white'
            : 'bg-bg-muted text-text-meta hover:bg-bg-hover hover:text-text-body'"
        >
          전체
        </button>
        <button
          v-for="cat in categories"
          :key="cat.categoryName"
          @click="selectCategory(cat.categoryName)"
          class="px-3 py-1.5 text-xs font-medium rounded-full transition-colors"
          :class="selectedCategory === cat.categoryName
            ? 'bg-brand-primary text-white'
            : 'bg-bg-muted text-text-meta hover:bg-bg-hover hover:text-text-body'"
        >
          {{ cat.categoryName }} ({{ cat.postCount }})
        </button>
      </div>

      <!-- 카테고리 로딩 -->
      <div v-else class="flex items-center gap-2 mb-8 text-text-meta text-sm">
        <div class="w-4 h-4 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
        카테고리 로딩 중...
      </div>

      <!-- Loading (초기) -->
      <div v-if="isInitialLoad && isLoading" class="flex justify-center py-24">
        <div class="text-center">
          <Spinner size="lg" class="mx-auto mb-4" />
          <p class="text-text-meta">게시글을 불러오는 중...</p>
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error && isEmpty" class="text-center py-16">
        <div class="text-status-error font-semibold mb-2">{{ error }}</div>
      </div>

      <!-- Empty -->
      <div v-else-if="isEmpty" class="text-center py-20">
        <h3 class="text-lg font-semibold text-text-heading mb-2">
          {{ selectedCategory ? `${selectedCategory} 카테고리에 게시글이 없습니다` : '게시글이 없습니다' }}
        </h3>
        <p class="text-text-meta text-sm">
          {{ selectedCategory ? '다른 카테고리를 선택해보세요.' : '첫 게시글을 작성해보세요!' }}
        </p>
      </div>

      <!-- Post List -->
      <div v-else>
        <div>
          <PostCard
            v-for="post in posts"
            :key="post.id"
            :post="post"
            @click="goToPost"
          />
        </div>

        <!-- Infinite Scroll Trigger -->
        <div
          v-if="hasMore"
          ref="loadMoreTrigger"
          class="flex items-center justify-center py-12"
        >
          <div v-if="isLoadingMore" class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
        </div>

        <!-- 모두 로드 완료 -->
        <div v-else class="text-center py-12">
          <span class="text-xs text-text-meta">모든 게시글을 불러왔습니다</span>
        </div>
      </div>
    </div>
  </div>
</template>
