<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { getPublishedPosts, getTrendingPosts, getFeed } from "../api/posts";
import type { PostSummaryResponse } from "../dto/post";
import type { PageResponse } from "@/types";
import { Button, Card, SearchBar, Spinner } from '@portal/design-vue';
import PostCard from '../components/PostCard.vue';
import { useApiError } from '@portal/design-vue';
import { useSearchStore } from '../stores/searchStore';
import { useFollowStore } from '../stores/followStore';
import { usePortalAuth } from '@portal/vue-bridge';

const router = useRouter();
const route = useRoute();
const { isAuthenticated } = usePortalAuth();
const searchStore = useSearchStore();
const followStore = useFollowStore();
const { getErrorMessage } = useApiError();

// 탭 관련 상태
type TabType = 'feed' | 'trending' | 'recent';
type PeriodType = 'today' | 'week' | 'month' | 'year';

const currentTab = ref<TabType>('trending');
const currentPeriod = ref<PeriodType>('week');

const tabs = computed(() => {
  const items: { label: string; value: TabType }[] = [];
  if (isAuthenticated.value) {
    items.push({ label: '피드', value: 'feed' });
  }
  items.push(
    { label: '트렌딩', value: 'trending' },
    { label: '최신', value: 'recent' },
  );
  return items;
});

// 일반 목록 상태
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

// 일반 게시글 목록 로드
async function loadPosts(page: number = 1, append: boolean = false) {
  try {
    if (append) {
      isLoadingMore.value = true;
    } else {
      isLoading.value = true;
    }

    error.value = null;

    let response: PageResponse<PostSummaryResponse>;

    if (currentTab.value === 'feed') {
      // 팔로잉 목록이 로드되지 않았으면 먼저 로드
      if (!followStore.followingIdsLoaded) {
        await followStore.loadFollowingIds();
      }

      // 팔로잉이 없으면 빈 응답 반환
      if (followStore.followingIds.length === 0) {
        response = {
          items: [],
          page: 1,
          size: pageSize.value,
          totalElements: 0,
          totalPages: 0,
        };
      } else {
        response = await getFeed(followStore.followingIds, page, pageSize.value);
      }
    } else if (currentTab.value === 'trending') {
      response = await getTrendingPosts(currentPeriod.value, page, pageSize.value);
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
    currentPage.value = 1;
    posts.value = [];
    hasMore.value = true;
    loadPosts(1, false);
  }
}

// 검색 실행
function handleSearch(keyword: string) {
  searchStore.search(keyword);
}

// 검색 초기화
function handleClearSearch() {
  searchStore.clear();
  if (posts.value.length === 0) {
    loadPosts(1, false);
  }
}

// 게시글 클릭
function goToPost(postId: string) {
  router.push(`/${postId}`);
}

// 탭 변경
function changeTab(tab: TabType) {
  if (currentTab.value === tab) return;

  currentTab.value = tab;
  currentPage.value = 1;
  posts.value = [];
  hasMore.value = true;

  updateQueryParams();
  loadPosts(1, false);
}

// 기간 변경
function changePeriod(period: PeriodType) {
  if (currentPeriod.value === period) return;

  currentPeriod.value = period;
  currentPage.value = 1;
  posts.value = [];
  hasMore.value = true;

  updateQueryParams();
  loadPosts(1, false);
}

// URL 쿼리 파라미터 업데이트
function updateQueryParams() {
  const query: Record<string, string> = { tab: currentTab.value };

  if (currentTab.value === 'trending') {
    query.period = currentPeriod.value;
  }

  router.replace({ query });
}

// URL 쿼리 파라미터로부터 초기 상태 설정
function initializeFromQuery() {
  const { tab, period } = route.query;

  if (tab === 'feed' || tab === 'trending' || tab === 'recent') {
    if (tab === 'feed' && !isAuthenticated.value) {
      currentTab.value = 'trending';
    } else {
      currentTab.value = tab as TabType;
    }
  }

  if (period === 'today' || period === 'week' || period === 'month' || period === 'year') {
    currentPeriod.value = period as PeriodType;
  }
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
      <header class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-bold text-text-heading">블로그</h1>
          <p v-if="isSearchMode" class="text-sm text-text-meta mt-1">
            "{{ searchStore.keyword }}" 검색 결과
          </p>
        </div>
        <Button
          v-if="isAuthenticated"
          variant="primary"
          size="sm"
          @click="router.push('/write')"
        >
          글 작성
        </Button>
      </header>

      <!-- SearchBar -->
      <div class="mb-6">
        <SearchBar
          v-model="searchStore.keyword"
          placeholder="제목, 내용, 태그로 검색..."
          :loading="searchStore.isSearching"
          @search="handleSearch"
          @clear="handleClearSearch"
        />
      </div>

      <!-- 탭 (검색 모드가 아닐 때) -->
      <div v-if="!isSearchMode" class="mb-8">
        <!-- 탭 바 -->
        <div class="flex items-center gap-6 border-b border-border-default">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="pb-3 px-1 text-base font-medium transition-all border-b-2"
            :class="currentTab === tab.value
              ? 'text-brand-primary border-brand-primary'
              : 'text-text-meta border-transparent hover:text-text-heading hover:border-border-hover'"
            @click="changeTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 기간 필터 (트렌딩 탭) -->
        <div v-if="currentTab === 'trending'" class="flex items-center gap-2 mt-4">
          <button
            v-for="period in (['today', 'week', 'month', 'year'] as const)"
            :key="period"
            @click="changePeriod(period)"
            class="px-3 py-1.5 text-xs font-medium rounded-full transition-colors"
            :class="currentPeriod === period
              ? 'bg-brand-primary text-white'
              : 'bg-bg-muted text-text-meta hover:bg-bg-hover hover:text-text-body'"
          >
            {{ { today: '오늘', week: '이번 주', month: '이번 달', year: '올해' }[period] }}
          </button>
        </div>
      </div>

      <!-- Loading State (초기 로드) -->
      <div v-if="isInitialLoad && isLoading" class="flex justify-center py-24" data-testid="feed-loading">
        <div class="text-center">
          <Spinner size="lg" class="mx-auto mb-4" />
          <p class="text-text-meta">게시글을 불러오는 중...</p>
        </div>
      </div>

      <!-- Error State -->
      <Card v-else-if="currentError && isEmpty" class="bg-status-error-bg border-status-error/20 py-16 text-center" data-testid="feed-error">
        <div class="text-status-error font-semibold mb-2">{{ currentError }}</div>
        <Button variant="secondary" size="sm" class="mt-4" @click="refresh" data-testid="retry-button">
          다시 시도
        </Button>
      </Card>

      <!-- Empty State -->
      <div v-else-if="isEmpty" class="text-center py-20" data-testid="empty-feed">
        <h3 class="text-lg font-semibold text-text-heading mb-2">
          <template v-if="isSearchMode">검색 결과가 없습니다</template>
          <template v-else-if="currentTab === 'feed'">
            {{ followStore.followingIds.length === 0 ? '팔로우하는 사용자가 없습니다' : '피드가 비어있습니다' }}
          </template>
          <template v-else>아직 게시글이 없습니다</template>
        </h3>
        <p class="text-text-meta text-sm mb-6">
          <template v-if="isSearchMode">다른 검색어를 시도해보세요.</template>
          <template v-else-if="currentTab === 'feed'">
            {{ followStore.followingIds.length === 0 ? '관심 있는 사용자를 팔로우해보세요!' : '팔로우한 사용자들이 아직 게시글을 작성하지 않았습니다.' }}
          </template>
          <template v-else>첫 게시글을 작성해보세요!</template>
        </p>
        <Button
          v-if="!isSearchMode && isAuthenticated && currentTab !== 'feed'"
          variant="primary"
          size="sm"
          @click="router.push('/write')"
        >
          첫 글 작성하기
        </Button>
        <Button
          v-if="currentTab === 'feed' && followStore.followingIds.length === 0"
          variant="primary"
          size="sm"
          @click="changeTab('trending')"
          data-testid="go-to-trending"
        >
          트렌딩 게시글 보기
        </Button>
      </div>

      <!-- Post List (단일 컬럼 피드) -->
      <div v-else>
        <div>
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
          class="flex items-center justify-center py-12"
        >
          <div v-if="isLoadingMore || searchStore.isSearching" class="text-center" data-testid="loading-more">
            <div class="w-8 h-8 border-2 border-border-default border-t-brand-primary rounded-full animate-spin mx-auto"></div>
          </div>
        </div>

        <!-- 모두 로드 완료 -->
        <div v-else class="text-center py-12" data-testid="feed-end">
          <span class="text-xs text-text-meta">
            {{ isSearchMode ? '모든 검색 결과를 불러왔습니다' : '모든 게시글을 불러왔습니다' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
