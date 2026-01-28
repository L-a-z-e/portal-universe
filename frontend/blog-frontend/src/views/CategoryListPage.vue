<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getCategoryStats, getPostsByCategory, getPublishedPosts } from '../api/posts';
import type { CategoryStats, PostSummaryResponse, PageResponse } from '@/types';
import { Card } from '@portal/design-system-vue';
import PostCard from '../components/PostCard.vue';

const router = useRouter();
const route = useRoute();

// 카테고리 관련 상태
const categories = ref<CategoryStats[]>([]);
const selectedCategory = ref<string | null>(null); // null = 전체
const categoriesLoading = ref(false);
const categoriesError = ref<string | null>(null);

// 게시글 목록 상태
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
    categoriesError.value = '카테고리 목록을 불러올 수 없습니다.';
  } finally {
    categoriesLoading.value = false;
  }
}

// 게시글 목록 로드
async function loadPosts(page: number = 0, append: boolean = false) {
  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
    }

    error.value = null;

    let response: PageResponse<PostSummaryResponse>;

    if (selectedCategory.value) {
      // 특정 카테고리의 게시글
      response = await getPostsByCategory(selectedCategory.value, page, pageSize.value);
    } else {
      // 전체 게시글
      response = await getPublishedPosts(page, pageSize.value);
    }

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
  loadPosts(currentPage.value + 1, true);
}

// 카테고리 선택
function selectCategory(category: string | null) {
  if (selectedCategory.value === category) return;

  selectedCategory.value = category;
  currentPage.value = 0;
  posts.value = [];
  hasMore.value = true;

  // URL 쿼리 업데이트
  updateQueryParams();

  loadPosts(0, false);
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
  <div class="w-full min-h-screen">
    <div class="mx-auto px-6 sm:px-8 lg:px-12 py-8">
      <!-- Header -->
      <header class="mb-6">
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">
          📂 카테고리
        </h1>
        <p class="text-text-meta">
          {{ selectedCategory ? `${selectedCategory} - ${totalCount}개의 게시글` : `전체 ${totalCount}개의 게시글` }}
        </p>
      </header>

      <!-- 카테고리 에러 상태 -->
      <Card v-if="categoriesError" class="bg-status-error-bg border-status-error/20 py-8 text-center mb-6">
        <div class="text-status-error font-semibold">{{ categoriesError }}</div>
      </Card>

      <!-- 레이아웃: 반응형 그리드 (카테고리 목록 + 게시글 그리드) -->
      <div class="category-layout">
        <!-- 카테고리 사이드바 -->
        <aside class="category-sidebar">
          <!-- 카테고리 로딩 -->
          <Card v-if="categoriesLoading" class="p-6 text-center">
            <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
          </Card>

          <!-- 카테고리 목록 -->
          <div v-else class="category-list">
            <!-- 전체 카테고리 -->
            <Card
              hoverable
              @click="selectCategory(null)"
              class="category-card"
              :class="{ 'category-card-active': selectedCategory === null }"
            >
              <div class="category-card-content">
                <div class="category-icon">📝</div>
                <div class="category-info">
                  <h3 class="category-name">전체</h3>
                  <p class="category-count">모든 게시글</p>
                </div>
              </div>
            </Card>

            <!-- 개별 카테고리 -->
            <Card
              v-for="category in categories"
              :key="category.categoryName"
              hoverable
              @click="selectCategory(category.categoryName)"
              class="category-card"
              :class="{ 'category-card-active': selectedCategory === category.categoryName }"
            >
              <div class="category-card-content">
                <div class="category-icon">📁</div>
                <div class="category-info">
                  <h3 class="category-name">{{ category.categoryName }}</h3>
                  <p class="category-count">{{ category.postCount }}개 게시글</p>
                  <p v-if="category.latestPostDate" class="category-date">
                    최근: {{ new Date(category.latestPostDate).toLocaleDateString('ko-KR') }}
                  </p>
                </div>
              </div>
            </Card>
          </div>
        </aside>

        <!-- 게시글 목록 영역 -->
        <main class="posts-main">
          <!-- Loading State (초기 로드) -->
          <Card v-if="isInitialLoad && isLoading" class="text-center py-24 bg-bg-muted border-0 shadow-none">
            <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-5"></div>
            <p class="text-text-meta text-lg">게시글을 불러오는 중...</p>
          </Card>

          <!-- Error State -->
          <Card v-else-if="error && isEmpty" class="bg-status-error-bg border-status-error/20 py-16 text-center">
            <div class="text-4xl text-status-error mb-4">❌</div>
            <div class="text-status-error font-semibold text-lg mb-2">{{ error }}</div>
          </Card>

          <!-- Empty State -->
          <Card v-else-if="isEmpty" class="text-center py-20">
            <div class="text-6xl mb-4">📭</div>
            <h3 class="text-2xl font-bold text-text-heading mb-2">
              {{ selectedCategory ? `${selectedCategory} 카테고리에 게시글이 없습니다` : '게시글이 없습니다' }}
            </h3>
            <p class="text-text-meta mb-6">
              {{ selectedCategory ? '다른 카테고리를 선택해보세요.' : '첫 게시글을 작성해보세요!' }}
            </p>
          </Card>

          <!-- Post Grid -->
          <div v-else>
            <div class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4 gap-6">
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
              class="min-h-[100px] flex items-center justify-center mt-8"
            >
              <div v-if="isLoadingMore" class="text-center py-8">
                <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
                <p class="text-text-meta text-sm">더 많은 게시글을 불러오는 중...</p>
              </div>
            </div>

            <!-- 모두 로드 완료 -->
            <div v-else class="text-center py-8 mt-8">
              <div class="inline-flex items-center gap-2 px-4 py-2 bg-bg-muted rounded-full">
                <svg class="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                </svg>
                <span class="text-text-meta text-sm font-medium">모든 게시글을 불러왔습니다</span>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 카테고리 레이아웃 */
.category-layout {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 모바일: 카테고리가 상단 */
.category-sidebar {
  width: 100%;
}

.category-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

/* 데스크탑: 카테고리가 좌측 사이드바 */
@media (min-width: 1024px) {
  .category-layout {
    display: grid;
    grid-template-columns: 280px 1fr;
    gap: 2rem;
  }

  .category-sidebar {
    position: sticky;
    top: 1rem;
    align-self: start;
    max-height: calc(100vh - 2rem);
    overflow-y: auto;
  }

  .category-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
}

/* 카테고리 카드 */
.category-card {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.category-card:hover {
  border-color: var(--semantic-brand-primary);
}

.category-card-active {
  border-color: var(--semantic-brand-primary);
  background-color: var(--semantic-bg-muted);
}

.category-card-content {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
}

.category-icon {
  font-size: 2rem;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0 0 0.25rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-count {
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
  margin: 0;
}

.category-date {
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
  margin: 0.25rem 0 0 0;
}

/* 게시글 메인 영역 */
.posts-main {
  min-width: 0;
  flex: 1;
}

/* 스크롤바 스타일 (카테고리 사이드바용) */
.category-sidebar::-webkit-scrollbar {
  width: 6px;
}

.category-sidebar::-webkit-scrollbar-track {
  background: var(--semantic-bg-muted);
  border-radius: 3px;
}

.category-sidebar::-webkit-scrollbar-thumb {
  background: var(--semantic-border-default);
  border-radius: 3px;
}

.category-sidebar::-webkit-scrollbar-thumb:hover {
  background: var(--semantic-text-meta);
}
</style>
