<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner } from '@portal/design-vue';
import { getAuthorCategoryStats } from '@/api/posts';
import type { CategoryStats } from '@/dto/post';

interface Props {
  authorId: string;
}
const props = defineProps<Props>();
const router = useRouter();

const categories = ref<CategoryStats[]>([]);
const loading = ref(false);
const error = ref('');

const fetchCategories = async () => {
  loading.value = true;
  error.value = '';
  try {
    categories.value = await getAuthorCategoryStats(props.authorId);
  } catch (err: any) {
    error.value = '카테고리 통계를 불러오는데 실패했습니다.';
    console.error('Failed to fetch author categories:', err);
  } finally {
    loading.value = false;
  }
};

const handleCategoryClick = (categoryName: string) => {
  router.push(`/categories?highlight=${encodeURIComponent(categoryName)}`);
};

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('ko-KR');
};

onMounted(fetchCategories);
watch(() => props.authorId, fetchCategories);
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
    <div v-else-if="categories.length === 0" class="flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-16 h-16 text-text-meta mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A2 2 0 013 12V7a4 4 0 014-4z" />
      </svg>
      <p class="text-text-meta">카테고리가 없습니다.</p>
    </div>

    <!-- 카테고리 리스트 -->
    <div v-else class="space-y-2">
      <button
        v-for="cat in categories"
        :key="cat.categoryName"
        class="w-full flex items-center justify-between px-4 py-3 rounded-lg border border-border-default hover:border-brand-primary/30 hover:bg-brand-primary/5 transition-all text-left cursor-pointer"
        @click="handleCategoryClick(cat.categoryName)"
      >
        <div class="flex-1 min-w-0">
          <span class="text-sm font-medium text-text-heading">{{ cat.categoryName }}</span>
          <span class="text-xs text-text-meta ml-2">최근 {{ formatDate(cat.latestPostDate) }}</span>
        </div>
        <span class="text-xs font-semibold text-brand-primary bg-brand-primary/10 px-2.5 py-1 rounded-full shrink-0 ml-3">
          {{ cat.postCount }}개
        </span>
      </button>
    </div>
  </div>
</template>
