<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner, Card } from '@portal/design-vue';
import { getSeriesList } from '@/api/series';
import type { SeriesListResponse } from '@/dto/series';

interface Props {
  authorId: string;
}
const props = defineProps<Props>();
const router = useRouter();

const seriesList = ref<SeriesListResponse[]>([]);
const loading = ref(false);
const error = ref('');

const fetchSeries = async () => {
  loading.value = true;
  error.value = '';
  try {
    seriesList.value = await getSeriesList(props.authorId);
  } catch (err: any) {
    error.value = '시리즈를 불러오는데 실패했습니다.';
    console.error('Failed to fetch author series:', err);
  } finally {
    loading.value = false;
  }
};

const goToSeries = (seriesId: string) => {
  router.push(`/series/${seriesId}`);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('ko-KR');
};

onMounted(fetchSeries);
watch(() => props.authorId, fetchSeries);
</script>

<template>
  <div class="py-4">
    <!-- 로딩 -->
    <div v-if="loading" class="flex justify-center py-16">
      <Spinner size="md" />
    </div>

    <!-- 에러 -->
    <p v-else-if="error" class="text-center text-text-meta py-16">{{ error }}</p>

    <!-- 빈 상태 -->
    <div v-else-if="seriesList.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-16 h-16 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
      <p class="text-text-meta">시리즈가 없습니다.</p>
    </div>

    <!-- 시리즈 그리드 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <Card
        v-for="series in seriesList"
        :key="series.id"
        class="cursor-pointer transition-all hover:shadow-md hover:-translate-y-0.5"
        @click="goToSeries(series.id)"
      >
        <div class="p-4">
          <h3 class="text-base font-bold text-text-heading mb-1 truncate">{{ series.name }}</h3>
          <p v-if="series.description" class="text-sm text-text-body mb-3 line-clamp-2">{{ series.description }}</p>
          <div class="flex justify-between text-xs text-text-meta">
            <span class="font-semibold">게시글 {{ series.postCount }}개</span>
            <span>{{ formatDate(series.updatedAt) }}</span>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>
