<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Card } from '@portal/design-system-vue';
import { getPostNavigation } from '@/api/posts';
import type { PostNavigationResponse } from '@/types';
import { DEFAULT_THUMBNAILS } from '@/config/assets';

interface Props {
  postId: string;
  scope?: 'all' | 'author' | 'category' | 'series';
}
const props = withDefaults(defineProps<Props>(), {
  scope: 'all'
});

const router = useRouter();
const navigation = ref<PostNavigationResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);

// 네비게이션 데이터 로드
async function loadNavigation() {
  loading.value = true;
  error.value = null;
  try {
    navigation.value = await getPostNavigation(props.postId, props.scope);
  } catch (err) {
    console.error('Failed to load navigation:', err);
    error.value = '네비게이션을 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// 포스트로 이동
function navigateToPost(postId: string) {
  router.push({ name: 'PostDetail', params: { postId } });
}

// 썸네일 에러 핸들링
function getThumbnail(url?: string) {
  return url || DEFAULT_THUMBNAILS.write;
}

// postId prop 변경 감지 (부모에서 새 postId가 전달될 때 리로드)
watch(() => props.postId, () => {
  loadNavigation();
});

onMounted(() => {
  loadNavigation();
});
</script>

<template>
  <div class="post-navigation">
    <!-- Loading -->
    <div v-if="loading" class="nav-loading">
      <div class="loading-spinner"></div>
      <p>네비게이션을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="nav-error">
      {{ error }}
    </div>

    <!-- Navigation Cards -->
    <div
      v-else-if="navigation && (navigation.previousPost || navigation.nextPost)"
      class="nav-container"
    >
      <!-- Previous Post -->
      <Card
        v-if="navigation.previousPost"
        class="nav-card prev-card"
        hoverable
        @click="navigateToPost(navigation.previousPost.id)"
      >
        <div class="nav-content">
          <div class="nav-header">
            <svg class="nav-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
            <span class="nav-label">이전 글</span>
          </div>

          <div class="nav-body">
            <img
              :src="getThumbnail(navigation.previousPost.thumbnailUrl)"
              :alt="navigation.previousPost.title"
              class="nav-thumbnail"
            />
            <div class="nav-info">
              <h3 class="nav-title">{{ navigation.previousPost.title }}</h3>
              <p class="nav-date">
                {{ new Date(navigation.previousPost.publishedAt).toLocaleDateString('ko-KR') }}
              </p>
            </div>
          </div>
        </div>
      </Card>

      <!-- Placeholder for alignment -->
      <div v-else class="nav-placeholder"></div>

      <!-- Next Post -->
      <Card
        v-if="navigation.nextPost"
        class="nav-card next-card"
        hoverable
        @click="navigateToPost(navigation.nextPost.id)"
      >
        <div class="nav-content">
          <div class="nav-header">
            <span class="nav-label">다음 글</span>
            <svg class="nav-arrow" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </div>

          <div class="nav-body">
            <img
              :src="getThumbnail(navigation.nextPost.thumbnailUrl)"
              :alt="navigation.nextPost.title"
              class="nav-thumbnail"
            />
            <div class="nav-info">
              <h3 class="nav-title">{{ navigation.nextPost.title }}</h3>
              <p class="nav-date">
                {{ new Date(navigation.nextPost.publishedAt).toLocaleDateString('ko-KR') }}
              </p>
            </div>
          </div>
        </div>
      </Card>

      <!-- Placeholder for alignment -->
      <div v-else class="nav-placeholder"></div>
    </div>
  </div>
</template>

<style scoped>
.post-navigation {
  width: 100%;
}

/* Loading & Error */
.nav-loading,
.nav-error {
  text-align: center;
  padding: 2rem;
  color: var(--semantic-text-meta);
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 3px solid var(--semantic-border-muted);
  border-top-color: var(--semantic-brand-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.nav-error {
  color: var(--semantic-status-error);
}

/* Navigation Container */
.nav-container {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

@media (min-width: 768px) {
  .nav-container {
    grid-template-columns: 1fr 1fr;
  }
}

/* Navigation Card */
.nav-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: var(--semantic-brand-primary);
}

.nav-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Header */
.nav-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--semantic-brand-primary);
}

.prev-card .nav-header {
  justify-content: flex-start;
}

.next-card .nav-header {
  justify-content: flex-end;
}

.nav-arrow {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.nav-label {
  font-size: 0.875rem;
  font-weight: 600;
}

/* Body */
.nav-body {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.next-card .nav-body {
  flex-direction: row-reverse;
}

.nav-thumbnail {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 0.5rem;
  flex-shrink: 0;
  background: var(--semantic-bg-muted);
}

.nav-info {
  flex: 1;
  min-width: 0;
}

.next-card .nav-info {
  text-align: right;
}

.nav-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0 0 0.25rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.nav-date {
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
  margin: 0;
}

/* Placeholder */
.nav-placeholder {
  display: none;
}

@media (min-width: 768px) {
  .nav-placeholder {
    display: block;
  }
}

/* 모바일 최적화 */
@media (max-width: 640px) {
  .nav-thumbnail {
    width: 60px;
    height: 60px;
  }

  .nav-title {
    font-size: 0.875rem;
  }

  .nav-label {
    font-size: 0.8125rem;
  }
}
</style>
