<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { searchPostsAdvanced } from '@/api/posts';
import type { PostSummaryResponse, PostSearchRequest, PageResponse } from '@/types';
import { Button, Card, Input, Tag } from '@portal/design-system-vue';
import PostCard from '@/components/PostCard.vue';

const router = useRouter();

// 검색 필터 상태
const keyword = ref('');
const category = ref('');
const tagsInput = ref('');
const authorId = ref('');
const startDate = ref('');
const endDate = ref('');

// 결과 상태
const results = ref<PostSummaryResponse[]>([]);
const currentPage = ref(0);
const pageSize = ref(12);
const totalElements = ref(0);
const hasMore = ref(false);

// 로딩/에러 상태
const isLoading = ref(false);
const isLoadingMore = ref(false);
const error = ref<string | null>(null);

// 검색 실행 여부
const hasSearched = ref(false);

// 무한 스크롤 트리거 요소
const loadMoreTrigger = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 태그 배열 파싱
const parsedTags = computed(() => {
  if (!tagsInput.value.trim()) return [];
  return tagsInput.value
    .split(',')
    .map((tag) => tag.trim())
    .filter((tag) => tag.length > 0);
});

// 검색 조건이 비어있는지 확인
const isSearchEmpty = computed(() => {
  return (
    !keyword.value.trim() &&
    !category.value.trim() &&
    parsedTags.value.length === 0 &&
    !authorId.value.trim() &&
    !startDate.value &&
    !endDate.value
  );
});

// 빈 상태 확인
const isEmpty = computed(() => hasSearched.value && !isLoading.value && results.value.length === 0);

// 더 로드 가능 여부
const canLoadMore = computed(() => hasMore.value && !isLoadingMore.value && !isLoading.value);

// 검색 실행
async function handleSearch(page: number = 0, append: boolean = false) {
  if (isSearchEmpty.value) {
    error.value = '최소 하나의 검색 조건을 입력해주세요.';
    return;
  }

  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
      results.value = [];
    }

    error.value = null;

    const searchRequest: PostSearchRequest = {
      keyword: keyword.value.trim() || undefined,
      category: category.value.trim() || undefined,
      tags: parsedTags.value.length > 0 ? parsedTags.value : undefined,
      authorId: authorId.value.trim() || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
      page,
      size: pageSize.value,
    };

    const response: PageResponse<PostSummaryResponse> = await searchPostsAdvanced(searchRequest);

    if (append) {
      results.value = [...results.value, ...response.content];
    } else {
      results.value = response.content;
    }

    currentPage.value = response.number;
    totalElements.value = response.totalElements;
    hasMore.value = !response.last;
    hasSearched.value = true;
  } catch (err) {
    console.error('Failed to search posts:', err);
    error.value = '검색에 실패했습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    isLoading.value = false;
    isLoadingMore.value = false;
  }
}

// 다음 페이지 로드
function loadMore() {
  if (!canLoadMore.value) return;
  handleSearch(currentPage.value + 1, true);
}

// 검색 초기화
function resetSearch() {
  keyword.value = '';
  category.value = '';
  tagsInput.value = '';
  authorId.value = '';
  startDate.value = '';
  endDate.value = '';
  results.value = [];
  currentPage.value = 0;
  totalElements.value = 0;
  hasMore.value = false;
  error.value = null;
  hasSearched.value = false;
}

// 게시글 클릭
function goToPost(postId: string) {
  router.push(`/${postId}`);
}

// 뒤로가기
function goBack() {
  router.back();
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
      threshold: 0.1,
    }
  );

  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value);
  }
}

// 초기화
onMounted(() => {
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
        <div class="flex items-center gap-3 mb-4">
          <Button variant="ghost" size="sm" @click="goBack" class="text-text-meta hover:text-text-body">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
            뒤로가기
          </Button>
        </div>
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">🔍 고급 검색</h1>
        <p class="text-text-meta">
          {{ hasSearched ? `${totalElements}개의 검색 결과` : '다양한 조건으로 게시글을 검색하세요' }}
        </p>
      </header>

      <!-- 검색 폼 -->
      <Card class="mb-8" padding="lg">
        <form @submit.prevent="handleSearch(0, false)" class="space-y-4">
          <!-- 키워드 -->
          <div>
            <label for="keyword" class="block text-sm font-medium text-text-heading mb-2">
              키워드
            </label>
            <Input
              id="keyword"
              v-model="keyword"
              type="text"
              placeholder="제목, 내용에서 검색..."
              class="w-full"
            />
          </div>

          <!-- 카테고리 -->
          <div>
            <label for="category" class="block text-sm font-medium text-text-heading mb-2">
              카테고리
            </label>
            <Input
              id="category"
              v-model="category"
              type="text"
              placeholder="예: Tech, Travel, Lifestyle"
              class="w-full"
            />
          </div>

          <!-- 태그 -->
          <div>
            <label for="tags" class="block text-sm font-medium text-text-heading mb-2">
              태그 (쉼표로 구분)
            </label>
            <Input
              id="tags"
              v-model="tagsInput"
              type="text"
              placeholder="예: JavaScript, React, Vue"
              class="w-full"
            />
            <div v-if="parsedTags.length > 0" class="flex flex-wrap gap-2 mt-2">
              <Tag v-for="tag in parsedTags" :key="tag" variant="default" size="sm">
                {{ tag }}
              </Tag>
            </div>
          </div>

          <!-- 작성자 ID -->
          <div>
            <label for="authorId" class="block text-sm font-medium text-text-heading mb-2">
              작성자 ID
            </label>
            <Input
              id="authorId"
              v-model="authorId"
              type="text"
              placeholder="작성자 UUID"
              class="w-full"
            />
          </div>

          <!-- 날짜 범위 -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label for="startDate" class="block text-sm font-medium text-text-heading mb-2">
                시작일
              </label>
              <input id="startDate" v-model="startDate" type="date" class="w-full px-3 py-2 border border-border-default rounded-lg bg-bg-card text-text-body focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent" />
            </div>
            <div>
              <label for="endDate" class="block text-sm font-medium text-text-heading mb-2">
                종료일
              </label>
              <input id="endDate" v-model="endDate" type="date" class="w-full px-3 py-2 border border-border-default rounded-lg bg-bg-card text-text-body focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent" />
            </div>
          </div>

          <!-- 버튼 -->
          <div class="flex gap-3 pt-2">
            <Button type="submit" variant="primary" size="md" class="flex-1" :disabled="isLoading">
              {{ isLoading ? '검색 중...' : '검색' }}
            </Button>
            <Button
              type="button"
              variant="secondary"
              size="md"
              @click="resetSearch"
              :disabled="isLoading"
            >
              초기화
            </Button>
          </div>
        </form>
      </Card>

      <!-- Error State -->
      <Card
        v-if="error && !isLoading"
        class="bg-status-error-bg border-status-error/20 py-16 text-center mb-8"
      >
        <div class="text-4xl text-status-error mb-4">❌</div>
        <div class="text-status-error font-semibold text-lg mb-2">{{ error }}</div>
        <Button variant="secondary" class="mt-4" @click="handleSearch(0, false)">
          다시 시도
        </Button>
      </Card>

      <!-- Loading State (초기 로드) -->
      <Card
        v-else-if="isLoading && !isLoadingMore"
        class="text-center py-24 bg-bg-muted border-0 shadow-none"
      >
        <div
          class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-5"
        ></div>
        <p class="text-text-meta text-lg">검색 중...</p>
      </Card>

      <!-- Empty State -->
      <Card v-else-if="isEmpty" class="text-center py-20">
        <div class="text-6xl mb-4">🔍</div>
        <h3 class="text-2xl font-bold text-text-heading mb-2">검색 결과가 없습니다</h3>
        <p class="text-text-meta mb-6">다른 검색 조건을 시도해보세요.</p>
        <Button variant="secondary" @click="resetSearch"> 검색 조건 초기화 </Button>
      </Card>

      <!-- 검색 결과 -->
      <div v-else-if="results.length > 0">
        <div
          class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6"
        >
          <PostCard v-for="post in results" :key="post.id" :post="post" @click="goToPost" />
        </div>

        <!-- Infinite Scroll Trigger -->
        <div
          v-if="hasMore"
          ref="loadMoreTrigger"
          class="min-h-[100px] flex items-center justify-center mt-8"
        >
          <div v-if="isLoadingMore" class="text-center py-8">
            <div
              class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-3"
            ></div>
            <p class="text-text-meta text-sm">더 많은 게시글을 불러오는 중...</p>
          </div>
        </div>

        <!-- 모두 로드 완료 -->
        <div v-else class="text-center py-8 mt-8">
          <div class="inline-flex items-center gap-2 px-4 py-2 bg-bg-muted rounded-full">
            <svg class="w-5 h-5 text-brand-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M5 13l4 4L19 7"
              ></path>
            </svg>
            <span class="text-text-meta text-sm font-medium">
              모든 검색 결과를 불러왔습니다
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Additional styles if needed */
</style>
