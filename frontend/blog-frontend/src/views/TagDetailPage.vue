<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getTagByName, getPostsByTag } from '../api/tags';
import type { TagResponse, PostSummaryResponse, PageResponse } from '@/types';
import PostCard from '../components/PostCard.vue';
import { Button } from '@portal/design-vue';

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
    error.value = '게시글 목록을 불러올 수 없습니다.';
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
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- 뒤로가기 -->
      <Button variant="ghost" size="sm" @click="goToTagList" class="mb-6">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        태그 목록
      </Button>

      <!-- 태그 정보 헤더 -->
      <header class="mb-8 pb-8 border-b border-border-default">
        <div class="flex items-center gap-3 mb-3">
          <h1 class="text-2xl font-bold text-text-heading">#{{ decodeURIComponent(tagName) }}</h1>
          <span v-if="tag" class="px-2.5 py-0.5 bg-brand-primary/10 text-brand-primary text-sm rounded-full font-medium">
            {{ tag.postCount }}개
          </span>
        </div>

        <p v-if="tag?.description" class="text-text-meta text-sm mb-4">{{ tag.description }}</p>

        <div v-if="tag" class="flex items-center gap-4 text-xs text-text-meta">
          <span>마지막 사용: {{ new Date(tag.lastUsedAt).toLocaleDateString('ko-KR') }}</span>
          <span>생성일: {{ new Date(tag.createdAt).toLocaleDateString('ko-KR') }}</span>
        </div>

        <div v-else-if="isLoadingTag" class="flex items-center gap-2 text-text-meta text-sm">
          <div class="w-4 h-4 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
          태그 정보 로딩 중...
        </div>

        <div v-else-if="tagError" class="text-sm text-status-error">{{ tagError }}</div>
      </header>

      <!-- 게시글 카운트 -->
      <div class="mb-4">
        <p class="text-sm text-text-meta">{{ totalElements }}개의 게시글</p>
      </div>

      <!-- Loading (초기) -->
      <div v-if="isLoading && posts.length === 0" class="flex justify-center py-24">
        <div class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error && posts.length === 0" class="text-center py-16">
        <div class="text-status-error font-semibold mb-2">{{ error }}</div>
        <Button variant="ghost" size="sm" @click="refresh" class="mt-2">
          다시 시도
        </Button>
      </div>

      <!-- Empty -->
      <div v-else-if="posts.length === 0 && !isLoading" class="text-center py-20">
        <h3 class="text-lg font-semibold text-text-heading mb-2">게시글이 없습니다</h3>
        <p class="text-text-meta text-sm">이 태그를 사용하는 게시글이 아직 없습니다.</p>
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
