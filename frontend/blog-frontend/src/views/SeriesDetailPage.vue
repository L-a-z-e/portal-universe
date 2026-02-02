<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Card, Button, Avatar } from '@portal/design-system-vue';
import { getSeriesById, getSeriesPosts } from '../api/series';
import type { SeriesResponse, PostSummaryResponse } from '@/types';
import { DEFAULT_THUMBNAILS } from '../config/assets';

const router = useRouter();
const route = useRoute();

const seriesId = computed(() => route.params.seriesId as string);
const series = ref<SeriesResponse | null>(null);
const posts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

// 썸네일 에러 핸들링
const imgError = ref(false);
const thumbnailSrc = computed(() => {
  if (imgError.value) {
    return DEFAULT_THUMBNAILS.write;
  }
  return series.value?.thumbnailUrl || DEFAULT_THUMBNAILS.write;
});

const onImgError = () => {
  imgError.value = true;
};

// 시리즈 데이터 로드
async function loadSeries() {
  loading.value = true;
  error.value = null;
  try {
    const [seriesData, postsData] = await Promise.all([
      getSeriesById(seriesId.value),
      getSeriesPosts(seriesId.value)
    ]);
    series.value = seriesData;
    posts.value = postsData;
  } catch (err) {
    console.error('Failed to load series:', err);
    error.value = '시리즈를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.';
  } finally {
    loading.value = false;
  }
}

// 포스트로 이동
function goToPost(postId: string) {
  router.push({ name: 'PostDetail', params: { postId } });
}

// 목록으로 돌아가기
function goBack() {
  router.push({ name: 'PostList' });
}

// 날짜 포맷팅
function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

// 상대 시간
function getRelativeTime(dateString: string): string {
  const now = new Date();
  const date = new Date(dateString);
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  return formatDate(dateString);
}

onMounted(() => {
  loadSeries();
});
</script>

<template>
  <div class="series-detail-page">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner">
        <svg class="animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p>시리즈를 불러오는 중...</p>
      </div>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="error-container">
      <Card class="error-card">
        <div class="error-content">
          <svg class="error-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p class="error-message">{{ error }}</p>
          <Button @click="goBack" variant="primary">목록으로 돌아가기</Button>
        </div>
      </Card>
    </div>

    <!-- 시리즈 내용 -->
    <div v-else-if="series" class="series-content">
      <!-- 헤더 -->
      <div class="series-header">
        <Button
            variant="ghost"
            size="sm"
            @click="goBack"
            class="back-button"
        >
          <svg class="back-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          목록으로
        </Button>
      </div>

      <!-- 시리즈 정보 카드 -->
      <Card class="series-info-card">
        <div class="series-info-content">
          <div class="thumbnail-section">
            <img
                :src="thumbnailSrc"
                :alt="series.name"
                class="series-thumbnail"
                @error="onImgError"
            />
          </div>

          <div class="info-section">
            <div class="series-badge">
              <svg class="badge-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
              시리즈
            </div>

            <h1 class="series-title">{{ series.name }}</h1>

            <p v-if="series.description" class="series-description">
              {{ series.description }}
            </p>

            <div class="series-meta">
              <div class="author-info">
                <Avatar :name="series.authorName" size="sm" />
                <span class="author-name">{{ series.authorName }}</span>
              </div>
              <div class="stats-info">
                <span class="stat-item">{{ series.postCount }}개의 글</span>
                <span class="separator">·</span>
                <span class="stat-item">{{ formatDate(series.updatedAt) }} 업데이트</span>
              </div>
            </div>
          </div>
        </div>
      </Card>

      <!-- 포스트 목록 -->
      <div class="posts-section">
        <h2 class="posts-title">시리즈 목록</h2>

        <div class="posts-list">
          <Card
              v-for="(post, index) in posts"
              :key="post.id"
              hoverable
              @click="goToPost(post.id)"
              class="post-item"
          >
            <div class="post-number">{{ index + 1 }}</div>
            <div class="post-content">
              <h3 class="post-title">{{ post.title }}</h3>
              <p v-if="post.summary" class="post-summary">{{ post.summary }}</p>
              <div class="post-meta">
                <span class="post-date">{{ getRelativeTime(post.publishedAt) }}</span>
                <span class="separator">·</span>
                <span class="post-stat">
                  <svg class="stat-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  {{ post.viewCount ?? 0 }}
                </span>
                <span class="post-stat">
                  <svg class="stat-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {{ post.likeCount ?? 0 }}
                </span>
              </div>
            </div>
            <div class="post-thumbnail" v-if="post.thumbnailUrl">
              <img :src="post.thumbnailUrl" :alt="post.title" />
            </div>
          </Card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.series-detail-page {
  max-width: 1024px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

/* 로딩 상태 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  color: var(--semantic-text-meta);
}

.loading-spinner svg {
  width: 3rem;
  height: 3rem;
  color: var(--semantic-brand-primary);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-spin {
  animation: spin 1s linear infinite;
}

/* 에러 상태 */
.error-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.error-card {
  max-width: 500px;
  width: 100%;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  text-align: center;
  padding: 2rem;
}

.error-icon {
  width: 3rem;
  height: 3rem;
  color: var(--semantic-status-error);
}

.error-message {
  color: var(--semantic-text-body);
  margin: 0;
}

/* 헤더 */
.series-header {
  margin-bottom: 1.5rem;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.back-icon {
  width: 1.25rem;
  height: 1.25rem;
}

/* 시리즈 정보 카드 */
.series-info-card {
  margin-bottom: 2rem;
}

.series-info-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.thumbnail-section {
  width: 100%;
  height: 240px;
  border-radius: 0.5rem;
  overflow: hidden;
  background-color: var(--semantic-bg-muted);
}

.series-thumbnail {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.series-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  background-color: var(--semantic-brand-primary-light, rgba(32, 201, 151, 0.1));
  color: var(--semantic-brand-primary);
  font-size: 0.875rem;
  font-weight: 600;
  border-radius: 0.375rem;
  width: fit-content;
}

.badge-icon {
  width: 1rem;
  height: 1rem;
}

.series-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0;
  line-height: 1.3;
}

.series-description {
  font-size: 1rem;
  line-height: 1.6;
  color: var(--semantic-text-body);
  margin: 0;
}

.series-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--semantic-border-muted);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.author-name {
  font-weight: 600;
  color: var(--semantic-text-body);
}

.stats-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
}

.separator {
  color: var(--semantic-text-meta);
}

/* 포스트 목록 */
.posts-section {
  margin-top: 2rem;
}

.posts-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0 0 1rem 0;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.post-item {
  display: flex;
  gap: 1rem;
  padding: 1.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.post-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.post-number {
  flex-shrink: 0;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--semantic-brand-primary-light, rgba(32, 201, 151, 0.1));
  color: var(--semantic-brand-primary);
  font-size: 1.125rem;
  font-weight: 700;
  border-radius: 0.5rem;
}

.post-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.post-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0;
  line-height: 1.5;
}

.post-item:hover .post-title {
  color: var(--semantic-brand-primary);
}

.post-summary {
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--semantic-text-body);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
}

.post-stat {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.stat-icon {
  width: 0.875rem;
  height: 0.875rem;
}

.post-thumbnail {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 0.375rem;
  overflow: hidden;
  background-color: var(--semantic-bg-muted);
}

.post-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 반응형 */
@media (min-width: 768px) {
  .series-info-content {
    flex-direction: row;
  }

  .thumbnail-section {
    width: 320px;
    height: 200px;
  }

  .info-section {
    flex: 1;
  }

  .series-title {
    font-size: 2.25rem;
  }
}

@media (max-width: 640px) {
  .post-thumbnail {
    display: none;
  }
}
</style>
