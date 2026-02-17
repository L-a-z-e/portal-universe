<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getTagByName, getPostsByTag } from '../api/tags';
import type { TagResponse, PostSummaryResponse, PageResponse } from '@/types';
import { Card, Button } from '@portal/design-vue';
import PostCard from '../components/PostCard.vue';

interface Props {
  tagName: string;
}

const props = defineProps<Props>();
const router = useRouter();

// 태그 상태
const tag = ref<TagResponse | null>(null);
const isLoadingTag = ref(false);
const tagError = ref<string | null>(null);

// 포스트 상태
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

// 무한 스크롤 트리거
const loadMoreTrigger = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 태그 색상 (해시 기반)
const tagColor = computed(() => {
  if (!tag.value) return 'bg-blue-500';

  const colors = [
    'bg-blue-500',
    'bg-green-500',
    'bg-purple-500',
    'bg-pink-500',
    'bg-violet-500',
    'bg-red-500',
    'bg-orange-500',
    'bg-cyan-500',
  ];

  let hash = 0;
  for (let i = 0; i < tag.value.name.length; i++) {
    hash = tag.value.name.charCodeAt(i) + ((hash << 5) - hash);
  }

  return colors[Math.abs(hash) % colors.length];
});

// 로드 가능 여부
const canLoadMore = computed(() => hasMore.value && !isLoadingMore.value && !isLoading.value);

// 태그 정보 로드
async function loadTag() {
  try {
    isLoadingTag.value = true;
    tagError.value = null;
    tag.value = await getTagByName(decodeURIComponent(props.tagName));
  } catch (err) {
    console.error('Failed to fetch tag:', err);
    tagError.value = '태그 정보를 불러올 수 없습니다.';
  } finally {
    isLoadingTag.value = false;
  }
}

// 포스트 목록 로드
async function loadPosts(page: number = 1, append: boolean = false) {
  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
    }

    error.value = null;

    const response: PageResponse<PostSummaryResponse> = await getPostsByTag(
      decodeURIComponent(props.tagName),
      page,
      pageSize.value
    );

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
    error.value = '게시글 목록을 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    isLoading.value = false;
    isLoadingMore.value = false;
  }
}

// 더 로드
function loadMore() {
  if (!canLoadMore.value) return;
  loadPosts(currentPage.value + 1, true);
}

// 새로고침
function refresh() {
  currentPage.value = 1;
  posts.value = [];
  hasMore.value = true;
  loadTag();
  loadPosts(1, false);
}

// 게시글 클릭
function goToPost(postId: string) {
  router.push(`/${postId}`);
}

// 태그 목록으로 돌아가기
function goToTagList() {
  router.push('/tags');
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
onMounted(async () => {
  await Promise.all([loadTag(), loadPosts(1, false)]);
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
      <!-- 뒤로가기 버튼 -->
      <Button
        variant="ghost"
        size="sm"
        @click="goToTagList"
        class="mb-6"
      >
        ← 태그 목록으로
      </Button>

      <!-- 태그 정보 헤더 -->
      <Card class="mb-8 overflow-hidden">
        <div :class="['h-32 relative', tagColor]">
          <div class="absolute inset-0 bg-gradient-to-br from-black/20 to-transparent"></div>
          <div class="absolute bottom-0 left-0 right-0 p-6 text-white">
            <h1 class="text-3xl sm:text-4xl font-bold mb-1">
              #{{ decodeURIComponent(tagName) }}
            </h1>
          </div>
        </div>

        <div v-if="tag" class="p-6">
          <div class="flex flex-wrap gap-6 items-center justify-between mb-4">
            <div class="flex gap-6">
              <div>
                <div class="text-2xl font-bold text-brand-primary">
                  {{ tag.postCount }}
                </div>
                <div class="text-text-meta text-sm">게시글</div>
              </div>
              <div>
                <div class="text-sm text-text-meta">
                  마지막 사용
                </div>
                <div class="text-sm font-medium">
                  {{ new Date(tag.lastUsedAt).toLocaleDateString('ko-KR') }}
                </div>
              </div>
              <div>
                <div class="text-sm text-text-meta">
                  생성일
                </div>
                <div class="text-sm font-medium">
                  {{ new Date(tag.createdAt).toLocaleDateString('ko-KR') }}
                </div>
              </div>
            </div>
          </div>

          <p v-if="tag.description" class="text-text-body">
            {{ tag.description }}
          </p>
        </div>

        <!-- 태그 로딩 -->
        <div v-else-if="isLoadingTag" class="p-6 text-center">
          <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
        </div>

        <!-- 태그 에러 -->
        <div v-else-if="tagError" class="p-6 text-center text-status-error">
          {{ tagError }}
        </div>
      </Card>

      <!-- 게시글 섹션 -->
      <div class="mb-6">
        <h2 class="text-2xl font-bold text-text-heading">
          이 태그의 게시글
        </h2>
        <p class="text-text-meta">
          {{ totalElements }}개의 게시글
        </p>
      </div>

      <!-- Loading State (초기 로드) -->
      <Card v-if="isLoading && posts.length === 0" class="text-center py-24 bg-bg-muted border-0 shadow-none">
        <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-5"></div>
        <p class="text-text-meta text-lg">게시글을 불러오는 중...</p>
      </Card>

      <!-- Error State -->
      <Card v-else-if="error && posts.length === 0" class="bg-status-error-bg border-status-error/20 py-16 text-center">
        <div class="text-4xl text-status-error mb-4">❌</div>
        <div class="text-status-error font-semibold text-lg mb-2">{{ error }}</div>
        <Button variant="secondary" class="mt-4" @click="refresh">
          다시 시도
        </Button>
      </Card>

      <!-- Empty State -->
      <Card v-else-if="posts.length === 0" class="text-center py-20">
        <div class="text-6xl mb-4">📭</div>
        <h3 class="text-2xl font-bold text-text-heading mb-2">
          게시글이 없습니다
        </h3>
        <p class="text-text-meta">
          이 태그를 사용하는 게시글이 아직 없습니다.
        </p>
      </Card>

      <!-- Post Grid -->
      <div v-else>
        <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
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
            <span class="text-text-meta text-sm font-medium">
              모든 게시글을 불러왔습니다
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Gradient overlay */
</style>
