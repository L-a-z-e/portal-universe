<script setup lang="ts">
import { useAuthStore } from "portal/stores";
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { getPublishedPosts, getTrendingPosts, getFeed } from "../api/posts";
import type { PostSummaryResponse } from "../dto/post";
import type { PageResponse } from "@/types";
import { Button, Card, SearchBar, Tabs, Spinner } from '@portal/design-system-vue';
import type { TabItem } from '@portal/design-system-vue';
import PostCard from '../components/PostCard.vue';
import { useSearchStore } from '../stores/searchStore';
import { useFollowStore } from '../stores/followStore';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const searchStore = useSearchStore();
const followStore = useFollowStore();

// 탭 관련 상태
type TabType = 'feed' | 'trending' | 'recent';
type PeriodType = 'today' | 'week' | 'month' | 'year';

const currentTab = ref<TabType>('trending');
const currentPeriod = ref<PeriodType>('week');

// Tab items for DS Tabs component
const tabItems = computed<TabItem[]>(() => {
  const items: TabItem[] = [];
  if (authStore.isAuthenticated) {
    items.push({ label: '📬 피드', value: 'feed' });
  }
  items.push(
    { label: '🔥 트렌딩', value: 'trending' },
    { label: '🕐 최신', value: 'recent' },
  );
  return items;
});

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

    let response: PageResponse<PostSummaryResponse>;

    if (currentTab.value === 'feed') {
      // 팔로잉 목록이 로드되지 않았으면 먼저 로드
      if (!followStore.followingIdsLoaded) {
        await followStore.loadFollowingIds();
      }

      // 팔로잉이 없으면 빈 응답 반환
      if (followStore.followingIds.length === 0) {
        response = {
          content: [],
          number: 0,
          size: pageSize.value,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: true,
          empty: true,
          numberOfElements: 0,
          pageable: {
            pageNumber: 0,
            pageSize: pageSize.value,
            sort: { empty: true, sorted: false, unsorted: true },
            offset: 0,
            paged: true,
            unpaged: false,
          },
          sort: { empty: true, sorted: false, unsorted: true },
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

// 탭 변경
function changeTab(tab: TabType) {
  if (currentTab.value === tab) return;

  currentTab.value = tab;
  currentPage.value = 0;
  posts.value = [];
  hasMore.value = true;

  // URL 쿼리 업데이트
  updateQueryParams();

  loadPosts(0, false);
}

// 기간 변경
function changePeriod(period: PeriodType) {
  if (currentPeriod.value === period) return;

  currentPeriod.value = period;
  currentPage.value = 0;
  posts.value = [];
  hasMore.value = true;

  // URL 쿼리 업데이트
  updateQueryParams();

  loadPosts(0, false);
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
    // 피드 탭은 로그인한 사용자만 접근 가능
    if (tab === 'feed' && !authStore.isAuthenticated) {
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
  <!-- ✅ 수정: max-w 제거, container 스타일 명확화 -->
  <div class="w-full min-h-screen">
    <!-- Inner Container: 최대 너비와 패딩 제어 -->
    <div class="mx-auto px-6 sm:px-8 lg:px-12 py-8">
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
      <div class="mb-6">
        <SearchBar
            v-model="searchStore.keyword"
            placeholder="제목, 내용, 태그로 검색..."
            :loading="searchStore.isSearching"
            @search="handleSearch"
            @clear="handleClearSearch"
        />
      </div>

      <!-- 탭 시스템 (검색 모드가 아닐 때만 표시) -->
      <div v-if="!isSearchMode" class="mb-6">
        <!-- 탭 버튼 -->
        <Tabs
          v-model="currentTab"
          :items="tabItems"
          class="mb-4"
          data-testid="post-list-tabs"
          @change="(tab: string) => changeTab(tab as TabType)"
        />

        <!-- 기간 필터 (트렌딩 탭일 때만 표시) -->
        <div v-if="currentTab === 'trending'" class="flex items-center gap-2">
          <button
              v-for="period in ['today', 'week', 'month', 'year']"
              :key="period"
              @click="changePeriod(period as PeriodType)"
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
      <Card v-if="isInitialLoad && isLoading" class="text-center py-24 bg-bg-muted border-0 shadow-none" data-testid="feed-loading">
        <Spinner size="lg" class="mx-auto mb-5" />
        <p class="text-text-meta text-lg">게시글을 불러오는 중...</p>
      </Card>

      <!-- Error State -->
      <Card v-else-if="currentError && isEmpty" class="bg-status-error-bg border-status-error/20 py-16 text-center" data-testid="feed-error">
        <div class="text-4xl text-status-error mb-4">❌</div>
        <div class="text-status-error font-semibold text-lg mb-2">{{ currentError }}</div>
        <Button variant="secondary" class="mt-4" @click="refresh" data-testid="retry-button">
          다시 시도
        </Button>
      </Card>

      <!-- Empty State -->
      <Card v-else-if="isEmpty" class="text-center py-20" data-testid="empty-feed">
        <div class="text-6xl mb-4">{{ isSearchMode ? '🔍' : (currentTab === 'feed' ? '👋' : '📭') }}</div>
        <h3 class="text-2xl font-bold text-text-heading mb-2">
          <template v-if="isSearchMode">검색 결과가 없습니다</template>
          <template v-else-if="currentTab === 'feed'">
            {{ followStore.followingIds.length === 0 ? '팔로우하는 사용자가 없습니다' : '피드가 비어있습니다' }}
          </template>
          <template v-else>아직 게시글이 없습니다</template>
        </h3>
        <p class="text-text-meta mb-6">
          <template v-if="isSearchMode">다른 검색어를 시도해보세요.</template>
          <template v-else-if="currentTab === 'feed'">
            {{ followStore.followingIds.length === 0 ? '관심 있는 사용자를 팔로우해보세요!' : '팔로우한 사용자들이 아직 게시글을 작성하지 않았습니다.' }}
          </template>
          <template v-else>첫 게시글을 작성해보세요!</template>
        </p>
        <Button
            v-if="!isSearchMode && authStore.isAuthenticated && currentTab !== 'feed'"
            variant="primary"
            @click="router.push('/write')"
        >
          첫 글 작성하기
        </Button>
        <Button
            v-if="currentTab === 'feed' && followStore.followingIds.length === 0"
            variant="primary"
            @click="changeTab('trending')"
            data-testid="go-to-trending"
        >
          트렌딩 게시글 보기
        </Button>
      </Card>

      <!-- Post Grid -->
      <div v-else>
        <!-- ✅ 수정: 반응형 그리드 브레이크포인트 명확화 -->
        <!--
          sm (640px):  1열
          md (768px):  2열
          lg (1024px): 3열
          xl (1280px): 4열
          2xl (1536px): 5열
        -->
        <div class="grid grid-cols-1 sm:grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
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
            class="min-h-[100px] flex items-center justify-center mt-8"
        >
          <div v-if="isLoadingMore || searchStore.isSearching" class="text-center py-8" data-testid="loading-more">
            <Spinner size="md" class="mx-auto mb-3" />
            <p class="text-text-meta text-sm">더 많은 게시글을 불러오는 중...</p>
          </div>
        </div>

        <!-- 모두 로드 완료 -->
        <div v-else class="text-center py-8 mt-8" data-testid="feed-end">
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
  </div>
</template>

<style scoped>
/* 그리드 디버깅용 (개발 중에만 사용) */
/*
.grid {
  border: 2px solid red;
}
.grid > * {
  border: 1px solid blue;
}
*/
</style>
