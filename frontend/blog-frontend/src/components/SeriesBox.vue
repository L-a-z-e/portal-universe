<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Card, Button } from '@portal/design-system-vue';
import { getSeriesById, getSeriesPosts } from '../api/series';
import type { SeriesResponse, PostSummaryResponse } from '@/types';

interface Props {
  seriesId: string;
  currentPostId: string;
}
const props = defineProps<Props>();

const router = useRouter();
const series = ref<SeriesResponse | null>(null);
const posts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

// 현재 포스트의 인덱스 계산
const currentIndex = computed(() => {
  if (!series.value) return -1;
  return series.value.postIds.indexOf(props.currentPostId);
});

// 현재 위치 표시 (1-based)
const positionText = computed(() => {
  if (currentIndex.value === -1) return '';
  return `${currentIndex.value + 1} / ${series.value?.postCount ?? 0}`;
});

// 이전/다음 포스트 ID
const previousPostId = computed(() => {
  if (currentIndex.value <= 0) return null;
  return series.value?.postIds[currentIndex.value - 1];
});

const nextPostId = computed(() => {
  if (!series.value || currentIndex.value === -1) return null;
  if (currentIndex.value >= series.value.postIds.length - 1) return null;
  return series.value.postIds[currentIndex.value + 1];
});

// 이전/다음 포스트 정보
const previousPost = computed(() => {
  if (!previousPostId.value) return null;
  return posts.value.find(p => p.id === previousPostId.value);
});

const nextPost = computed(() => {
  if (!nextPostId.value) return null;
  return posts.value.find(p => p.id === nextPostId.value);
});

// 시리즈 데이터 로드
async function loadSeries() {
  loading.value = true;
  error.value = null;
  try {
    const [seriesData, postsData] = await Promise.all([
      getSeriesById(props.seriesId),
      getSeriesPosts(props.seriesId)
    ]);
    series.value = seriesData;
    posts.value = postsData;
  } catch (err) {
    console.error('Failed to load series:', err);
    error.value = '시리즈 정보를 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// 포스트 이동
function navigateToPost(postId: string) {
  router.push({ name: 'PostDetail', params: { postId } });
}

// 시리즈 상세 페이지로 이동
function goToSeriesDetail() {
  router.push({ name: 'SeriesDetail', params: { seriesId: props.seriesId } });
}

onMounted(() => {
  loadSeries();
});
</script>

<template>
  <Card class="series-box" v-if="!loading && series">
    <div class="series-header">
      <div class="series-info">
        <div class="series-title-row">
          <svg class="series-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <h3 class="series-name">{{ series.name }}</h3>
        </div>
        <div class="series-position">{{ positionText }}</div>
      </div>

      <Button
          variant="ghost"
          size="sm"
          @click="goToSeriesDetail"
          class="view-all-button"
      >
        전체 목록 보기
      </Button>
    </div>

    <!-- 네비게이션 버튼 -->
    <div class="navigation-buttons">
      <Button
          v-if="previousPost"
          variant="outline"
          size="sm"
          @click="navigateToPost(previousPost.id)"
          class="nav-button prev-button"
      >
        <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
        <div class="nav-text">
          <span class="nav-label">이전 글</span>
          <span class="nav-title">{{ previousPost.title }}</span>
        </div>
      </Button>

      <Button
          v-if="nextPost"
          variant="outline"
          size="sm"
          @click="navigateToPost(nextPost.id)"
          class="nav-button next-button"
      >
        <div class="nav-text">
          <span class="nav-label">다음 글</span>
          <span class="nav-title">{{ nextPost.title }}</span>
        </div>
        <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </Button>
    </div>
  </Card>

  <!-- 로딩 상태 -->
  <Card v-else-if="loading" class="series-box loading">
    <div class="loading-content">시리즈 정보를 불러오는 중...</div>
  </Card>

  <!-- 에러 상태 -->
  <Card v-else-if="error" class="series-box error">
    <div class="error-content">{{ error }}</div>
  </Card>
</template>

<style scoped>
.series-box {
  background: linear-gradient(135deg, rgba(32, 201, 151, 0.05) 0%, rgba(32, 201, 151, 0.02) 100%);
  border: 1px solid var(--semantic-brand-primary, #20c997);
}

/* 헤더 */
.series-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--semantic-border-muted);
}

.series-info {
  flex: 1;
  min-width: 0;
}

.series-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.series-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: var(--semantic-brand-primary, #20c997);
  flex-shrink: 0;
}

.series-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.series-position {
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
  font-weight: 500;
}

.view-all-button {
  flex-shrink: 0;
}

/* 네비게이션 버튼 */
.navigation-buttons {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.nav-button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  text-align: left;
  transition: all 0.2s ease;
}

.nav-button:hover {
  background-color: var(--semantic-bg-hover);
  border-color: var(--semantic-brand-primary, #20c997);
}

.prev-button {
  justify-content: flex-start;
}

.next-button {
  justify-content: flex-end;
}

.nav-icon {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}

.nav-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
  flex: 1;
}

.nav-label {
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
  font-weight: 500;
}

.nav-title {
  font-size: 0.875rem;
  color: var(--semantic-text-body);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 로딩/에러 상태 */
.loading-content,
.error-content {
  text-align: center;
  padding: 1rem;
  color: var(--semantic-text-meta);
}

.error-content {
  color: var(--semantic-error, #f56565);
}

/* 반응형 */
@media (min-width: 768px) {
  .navigation-buttons {
    flex-direction: row;
  }

  .nav-button {
    flex: 1;
  }
}
</style>
