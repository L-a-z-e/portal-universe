<script setup lang="ts">
import { onMounted, ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Avatar, Button, Spinner } from '@portal/design-vue';
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
    error.value = '시리즈를 불러올 수 없습니다.';
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
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- 로딩 -->
      <div v-if="loading" class="flex flex-col items-center justify-center min-h-[50vh]">
        <Spinner size="lg" class="mb-4" />
        <p class="text-text-meta text-sm">시리즈를 불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="flex flex-col items-center justify-center py-24 text-center">
        <svg class="w-12 h-12 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <p class="text-text-body mb-4">{{ error }}</p>
        <Button @click="goBack" variant="primary" size="sm">목록으로 돌아가기</Button>
      </div>

      <!-- 시리즈 내용 -->
      <template v-else-if="series">
        <!-- 뒤로가기 -->
        <button
          @click="goBack"
          class="text-sm text-text-meta hover:text-text-heading transition-colors mb-6 inline-flex items-center gap-1"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          목록으로
        </button>

        <!-- 시리즈 정보 -->
        <header class="mb-10">
          <!-- 썸네일 -->
          <div class="w-full h-48 rounded-lg overflow-hidden bg-bg-muted mb-6">
            <img
              :src="thumbnailSrc"
              :alt="series.name"
              class="w-full h-full object-cover"
              @error="onImgError"
            />
          </div>

          <!-- 배지 -->
          <span class="inline-flex items-center gap-1.5 px-2.5 py-1 bg-brand-primary/10 text-brand-primary text-xs font-semibold rounded-full mb-3">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            시리즈
          </span>

          <h1 class="text-3xl font-bold text-text-heading tracking-tight mb-3">{{ series.name }}</h1>

          <p v-if="series.description" class="text-text-body leading-relaxed mb-4">
            {{ series.description }}
          </p>

          <div class="flex flex-wrap items-center gap-4 pt-4 border-t border-border-default">
            <div class="flex items-center gap-2">
              <Avatar :name="series.authorName" size="sm" />
              <span class="text-sm font-medium text-text-body">{{ series.authorName }}</span>
            </div>
            <div class="flex items-center gap-2 text-xs text-text-meta">
              <span>{{ series.postCount }}개의 글</span>
              <span class="w-1 h-1 rounded-full bg-border-default"></span>
              <span>{{ formatDate(series.updatedAt) }} 업데이트</span>
            </div>
          </div>
        </header>

        <!-- 포스트 목록 -->
        <section>
          <h2 class="text-lg font-semibold text-text-heading mb-4">시리즈 목록</h2>

          <div class="space-y-0">
            <button
              v-for="(post, index) in posts"
              :key="post.id"
              @click="goToPost(post.id)"
              class="w-full flex items-start gap-4 py-5 border-b border-border-default hover:bg-bg-hover transition-colors text-left group"
            >
              <!-- 번호 -->
              <span class="flex-shrink-0 w-8 h-8 flex items-center justify-center bg-brand-primary/10 text-brand-primary text-sm font-bold rounded-lg">
                {{ index + 1 }}
              </span>

              <!-- 콘텐츠 -->
              <div class="flex-1 min-w-0">
                <h3 class="text-base font-semibold text-text-heading group-hover:text-brand-primary transition-colors line-clamp-1 mb-1">
                  {{ post.title }}
                </h3>
                <p v-if="post.summary" class="text-sm text-text-meta line-clamp-2 mb-2">
                  {{ post.summary }}
                </p>
                <div class="flex items-center gap-3 text-xs text-text-meta">
                  <span>{{ getRelativeTime(post.publishedAt) }}</span>
                  <span class="flex items-center gap-1">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    {{ post.viewCount ?? 0 }}
                  </span>
                  <span class="flex items-center gap-1">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                    {{ post.likeCount ?? 0 }}
                  </span>
                </div>
              </div>

              <!-- 썸네일 -->
              <div v-if="post.thumbnailUrl" class="flex-shrink-0 w-28 h-20 rounded-lg overflow-hidden bg-bg-muted hidden sm:block">
                <img :src="post.thumbnailUrl" :alt="post.title" class="w-full h-full object-cover" />
              </div>
            </button>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>
