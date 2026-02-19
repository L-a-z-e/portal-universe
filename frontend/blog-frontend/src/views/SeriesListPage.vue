<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner, Card } from '@portal/design-vue';
import { getSeriesList } from '@/api/series';
import type { SeriesListResponse } from '@/dto/series';

const router = useRouter();

const seriesList = ref<SeriesListResponse[]>([]);
const loading = ref(false);
const error = ref('');
const sortBy = ref<'latest' | 'popular'>('latest');

const fetchSeries = async () => {
  loading.value = true;
  error.value = '';
  try {
    seriesList.value = await getSeriesList();
  } catch (err: any) {
    error.value = '시리즈 목록을 불러오는데 실패했습니다.';
    console.error('Failed to fetch series list:', err);
  } finally {
    loading.value = false;
  }
};

const sortedSeries = () => {
  const list = [...seriesList.value];
  if (sortBy.value === 'popular') {
    return list.sort((a, b) => b.postCount - a.postCount);
  }
  return list.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
};

const goToSeries = (seriesId: string) => {
  router.push(`/series/${seriesId}`);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('ko-KR');
};

onMounted(fetchSeries);
</script>

<template>
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 pt-16 pb-32">
      <!-- 헤더 -->
      <div class="flex items-center justify-between mb-8">
        <h1 class="text-2xl font-bold text-text-heading">시리즈</h1>
        <div class="flex gap-2">
          <button
            v-for="option in [{ label: '최신순', value: 'latest' as const }, { label: '인기순', value: 'popular' as const }]"
            :key="option.value"
            class="px-3 py-1.5 rounded-full text-sm font-medium transition-colors"
            :class="sortBy === option.value
              ? 'bg-brand-primary text-white'
              : 'bg-bg-muted text-text-meta hover:text-text-heading'"
            @click="sortBy = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center py-16">
        <Spinner size="lg" />
      </div>

      <!-- 에러 -->
      <p v-else-if="error" class="text-center text-text-meta py-16">{{ error }}</p>

      <!-- 빈 상태 -->
      <div v-else-if="seriesList.length === 0" class="flex flex-col items-center justify-center py-24 text-center">
        <svg class="w-16 h-16 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        <p class="text-text-meta text-lg">아직 시리즈가 없습니다.</p>
      </div>

      <!-- 시리즈 리스트 -->
      <div v-else class="space-y-4">
        <Card
          v-for="series in sortedSeries()"
          :key="series.id"
          class="cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5"
          @click="goToSeries(series.id)"
        >
          <div class="p-5">
            <div class="flex items-start justify-between gap-4">
              <div class="flex-1 min-w-0">
                <h2 class="text-lg font-bold text-text-heading mb-1 truncate">{{ series.name }}</h2>
                <p v-if="series.description" class="text-sm text-text-body mb-3 line-clamp-2">{{ series.description }}</p>
                <div class="flex items-center gap-3 text-xs text-text-meta">
                  <component
                    :is="series.authorUsername ? 'router-link' : 'span'"
                    :to="series.authorUsername ? `/@${series.authorUsername}` : undefined"
                    @click.stop
                    class="text-text-body"
                    :class="{ 'hover:text-brand-primary cursor-pointer transition-colors': series.authorUsername }"
                  >
                    {{ series.authorNickname }}
                  </component>
                  <span>·</span>
                  <span class="font-semibold">게시글 {{ series.postCount }}개</span>
                  <span>·</span>
                  <span>{{ formatDate(series.updatedAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>
