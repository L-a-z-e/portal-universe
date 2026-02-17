<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { searchPostsAdvanced } from '@/api/posts';
import type { PostSummaryResponse, PostSearchRequest, PageResponse } from '@/types';
import { Button, Input, Tag } from '@portal/design-vue';
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
const currentPage = ref(1);
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
async function handleSearch(page: number = 1, append: boolean = false) {
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
      results.value = [...results.value, ...response.items];
    } else {
      results.value = response.items;
    }

    currentPage.value = response.page;
    totalElements.value = response.totalElements;
    hasMore.value = response.page < response.totalPages;
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
  currentPage.value = 1;
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
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Header -->
      <header class="mb-8">
        <Button variant="ghost" size="sm" @click="goBack" class="mb-4">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          뒤로가기
        </Button>
        <h1 class="text-2xl font-bold text-text-heading mb-1">고급 검색</h1>
        <p class="text-sm text-text-meta">
          {{ hasSearched ? `${totalElements}개의 검색 결과` : '다양한 조건으로 게시글을 검색하세요' }}
        </p>
      </header>

      <!-- 검색 폼 -->
      <div class="mb-8 p-6 rounded-xl bg-bg-elevated border border-border-default">
        <form @submit.prevent="handleSearch(1, false)" class="space-y-4">
          <!-- 키워드 -->
          <div>
            <label for="keyword" class="block text-sm font-medium text-text-heading mb-2">키워드</label>
            <Input id="keyword" v-model="keyword" type="text" placeholder="제목, 내용에서 검색..." class="w-full" />
          </div>

          <!-- 카테고리 -->
          <div>
            <label for="category" class="block text-sm font-medium text-text-heading mb-2">카테고리</label>
            <Input id="category" v-model="category" type="text" placeholder="예: Tech, Travel, Lifestyle" class="w-full" />
          </div>

          <!-- 태그 -->
          <div>
            <label for="tags" class="block text-sm font-medium text-text-heading mb-2">태그 (쉼표로 구분)</label>
            <Input id="tags" v-model="tagsInput" type="text" placeholder="예: JavaScript, React, Vue" class="w-full" />
            <div v-if="parsedTags.length > 0" class="flex flex-wrap gap-2 mt-2">
              <Tag v-for="tag in parsedTags" :key="tag" variant="default" size="sm">
                {{ tag }}
              </Tag>
            </div>
          </div>

          <!-- 작성자 ID -->
          <div>
            <label for="authorId" class="block text-sm font-medium text-text-heading mb-2">작성자 ID</label>
            <Input id="authorId" v-model="authorId" type="text" placeholder="작성자 UUID" class="w-full" />
          </div>

          <!-- 날짜 범위 -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label for="startDate" class="block text-sm font-medium text-text-heading mb-2">시작일</label>
              <Input id="startDate" v-model="startDate" type="date" class="w-full" />
            </div>
            <div>
              <label for="endDate" class="block text-sm font-medium text-text-heading mb-2">종료일</label>
              <Input id="endDate" v-model="endDate" type="date" class="w-full" />
            </div>
          </div>

          <!-- 버튼 -->
          <div class="flex gap-3 pt-2">
            <Button type="submit" variant="primary" size="md" class="flex-1" :disabled="isLoading">
              {{ isLoading ? '검색 중...' : '검색' }}
            </Button>
            <Button type="button" variant="secondary" size="md" @click="resetSearch" :disabled="isLoading">
              초기화
            </Button>
          </div>
        </form>
      </div>

      <!-- Error -->
      <div v-if="error && !isLoading" class="text-center py-8 mb-8">
        <div class="text-status-error font-semibold mb-2">{{ error }}</div>
        <Button variant="ghost" size="sm" @click="handleSearch(1, false)">다시 시도</Button>
      </div>

      <!-- Loading (초기) -->
      <div v-else-if="isLoading && !isLoadingMore" class="flex justify-center py-24">
        <div class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
      </div>

      <!-- Empty -->
      <div v-else-if="isEmpty" class="text-center py-20">
        <h3 class="text-lg font-semibold text-text-heading mb-2">검색 결과가 없습니다</h3>
        <p class="text-text-meta text-sm mb-4">다른 검색 조건을 시도해보세요.</p>
        <Button variant="ghost" size="sm" @click="resetSearch">검색 조건 초기화</Button>
      </div>

      <!-- 검색 결과 -->
      <div v-else-if="results.length > 0">
        <div>
          <PostCard v-for="post in results" :key="post.id" :post="post" @click="goToPost" />
        </div>

        <!-- Infinite Scroll Trigger -->
        <div v-if="hasMore" ref="loadMoreTrigger" class="flex items-center justify-center py-12">
          <div v-if="isLoadingMore" class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
        </div>

        <!-- 모두 로드 완료 -->
        <div v-else class="text-center py-12">
          <span class="text-xs text-text-meta">모든 검색 결과를 불러왔습니다</span>
        </div>
      </div>
    </div>
  </div>
</template>
