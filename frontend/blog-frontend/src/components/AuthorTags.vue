<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner } from '@portal/design-vue';
import { getAuthorTagStats } from '@/api/posts';
import type { TagStatsResponse } from '@/dto/tag';

interface Props {
  authorId: string;
}
const props = defineProps<Props>();
const router = useRouter();

const tags = ref<TagStatsResponse[]>([]);
const loading = ref(false);
const error = ref('');

const fetchTags = async () => {
  loading.value = true;
  error.value = '';
  try {
    tags.value = await getAuthorTagStats(props.authorId, 30);
  } catch (err: any) {
    error.value = '태그 통계를 불러오는데 실패했습니다.';
    console.error('Failed to fetch author tags:', err);
  } finally {
    loading.value = false;
  }
};

const handleTagClick = (tagName: string) => {
  router.push(`/tags/${encodeURIComponent(tagName)}`);
};

const maxCount = () => {
  if (tags.value.length === 0) return 1;
  return Math.max(...tags.value.map(t => t.postCount));
};

const getTagSize = (count: number): string => {
  const ratio = count / maxCount();
  if (ratio > 0.8) return 'text-lg font-bold';
  if (ratio > 0.5) return 'text-base font-semibold';
  if (ratio > 0.3) return 'text-sm font-medium';
  return 'text-sm';
};

onMounted(fetchTags);
watch(() => props.authorId, fetchTags);
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
    <div v-else-if="tags.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-16 h-16 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
      </svg>
      <p class="text-text-meta">태그가 없습니다.</p>
    </div>

    <!-- 태그 클라우드 -->
    <div v-else class="flex flex-wrap gap-2.5 py-2">
      <button
        v-for="tag in tags"
        :key="tag.name"
        :class="[
          'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-border-default',
          'hover:border-brand-primary/30 hover:bg-brand-primary/5 transition-all cursor-pointer',
          getTagSize(tag.postCount)
        ]"
        @click="handleTagClick(tag.name)"
      >
        <span class="text-text-heading">#{{ tag.name }}</span>
        <span class="text-xs text-text-meta">({{ tag.postCount }})</span>
      </button>
    </div>
  </div>
</template>
