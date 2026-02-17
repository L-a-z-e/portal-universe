<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { Spinner } from '@portal/design-vue';
import { getAuthorStats } from '@/api/posts';
import type { AuthorStats } from '@/dto/post';

interface Props {
  authorId: string;
}
const props = defineProps<Props>();

const stats = ref<AuthorStats | null>(null);
const loading = ref(false);
const error = ref('');

const fetchStats = async () => {
  loading.value = true;
  error.value = '';
  try {
    stats.value = await getAuthorStats(props.authorId);
  } catch (err: any) {
    error.value = '통계를 불러오는데 실패했습니다.';
    console.error('Failed to fetch author stats:', err);
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

const formatNumber = (num: number) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '만';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num.toString();
};

onMounted(fetchStats);
watch(() => props.authorId, fetchStats);
</script>

<template>
  <div class="py-4">
    <!-- 로딩 -->
    <div v-if="loading" class="flex justify-center py-16">
      <Spinner size="md" />
    </div>

    <!-- 에러 -->
    <p v-else-if="error" class="text-center text-text-meta py-16">{{ error }}</p>

    <!-- 통계 카드 -->
    <template v-else-if="stats">
      <div class="grid grid-cols-2 gap-4 mb-8">
        <!-- 총 게시글 -->
        <div class="p-5 rounded-xl border border-border-default bg-bg-card">
          <p class="text-xs text-text-meta mb-1">총 게시글</p>
          <p class="text-2xl font-bold text-text-heading">{{ formatNumber(stats.totalPosts) }}</p>
        </div>
        <!-- 발행됨 -->
        <div class="p-5 rounded-xl border border-border-default bg-bg-card">
          <p class="text-xs text-text-meta mb-1">발행됨</p>
          <p class="text-2xl font-bold text-brand-primary">{{ formatNumber(stats.publishedPosts) }}</p>
        </div>
        <!-- 총 조회수 -->
        <div class="p-5 rounded-xl border border-border-default bg-bg-card">
          <p class="text-xs text-text-meta mb-1">총 조회수</p>
          <p class="text-2xl font-bold text-text-heading">{{ formatNumber(stats.totalViews) }}</p>
        </div>
        <!-- 총 좋아요 -->
        <div class="p-5 rounded-xl border border-border-default bg-bg-card">
          <p class="text-xs text-text-meta mb-1">총 좋아요</p>
          <p class="text-2xl font-bold text-text-heading">{{ formatNumber(stats.totalLikes) }}</p>
        </div>
      </div>

      <!-- 날짜 정보 -->
      <div class="space-y-3 text-sm">
        <div class="flex justify-between py-2 border-b border-border-default">
          <span class="text-text-meta">첫 게시글</span>
          <span class="text-text-body">{{ formatDate(stats.firstPostDate) }}</span>
        </div>
        <div class="flex justify-between py-2">
          <span class="text-text-meta">최근 게시글</span>
          <span class="text-text-body">{{ formatDate(stats.lastPostDate) }}</span>
        </div>
      </div>
    </template>
  </div>
</template>
