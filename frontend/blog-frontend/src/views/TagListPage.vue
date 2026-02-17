<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAllTags } from '../api/tags';
import type { TagResponse } from '@/types';
import { Card, Button, Input, Spinner } from '@portal/design-vue';

const router = useRouter();

// 상태
const tags = ref<TagResponse[]>([]);
const isLoading = ref(false);
const error = ref<string | null>(null);

// 필터 및 정렬
const searchKeyword = ref('');
const sortOption = ref<'popular' | 'name' | 'latest'>('popular');

// 정렬된 태그 목록
const sortedTags = computed(() => {
  let filtered = tags.value;

  // 검색 필터
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    filtered = filtered.filter(tag =>
      tag.name.toLowerCase().includes(keyword) ||
      tag.description?.toLowerCase().includes(keyword)
    );
  }

  // 정렬
  const sorted = [...filtered];
  switch (sortOption.value) {
    case 'popular':
      sorted.sort((a, b) => b.postCount - a.postCount);
      break;
    case 'name':
      sorted.sort((a, b) => a.name.localeCompare(b.name));
      break;
    case 'latest':
      sorted.sort((a, b) =>
        new Date(b.lastUsedAt).getTime() - new Date(a.lastUsedAt).getTime()
      );
      break;
  }

  return sorted;
});

// 태그 크기 계산 (postCount 기반)
const getTagSize = (postCount: number): string => {
  const maxCount = Math.max(...tags.value.map(t => t.postCount), 1);
  const ratio = postCount / maxCount;

  if (ratio >= 0.8) return 'text-4xl font-bold';
  if (ratio >= 0.6) return 'text-3xl font-semibold';
  if (ratio >= 0.4) return 'text-2xl font-semibold';
  if (ratio >= 0.2) return 'text-xl font-medium';
  return 'text-lg';
};

// 태그 색상 (해시 기반)
const getTagColor = (tagName: string): string => {
  const colors = [
    'text-blue-600 hover:text-blue-700',
    'text-green-600 hover:text-green-700',
    'text-purple-600 hover:text-purple-700',
    'text-pink-600 hover:text-pink-700',
    'text-indigo-600 hover:text-indigo-700',
    'text-red-600 hover:text-red-700',
    'text-orange-600 hover:text-orange-700',
    'text-teal-600 hover:text-teal-700',
  ];

  let hash = 0;
  for (let i = 0; i < tagName.length; i++) {
    hash = tagName.charCodeAt(i) + ((hash << 5) - hash);
  }

  const index = Math.abs(hash) % colors.length;
  return colors[index] as string;
};

// 태그 로드
async function loadTags() {
  try {
    isLoading.value = true;
    error.value = null;
    tags.value = await getAllTags();
  } catch (err) {
    console.error('Failed to fetch tags:', err);
    error.value = '태그 목록을 불러올 수 없습니다.';
  } finally {
    isLoading.value = false;
  }
}

// 태그 클릭
function goToTag(tagName: string) {
  router.push(`/tags/${encodeURIComponent(tagName)}`);
}

// 날짜 포맷팅
function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

// 초기화
onMounted(() => {
  loadTags();
});
</script>

<template>
  <div class="w-full min-h-screen">
    <div class="mx-auto px-6 sm:px-8 lg:px-12 py-8">
      <!-- Header -->
      <header class="mb-8">
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">
          🏷️ 태그
        </h1>
        <p class="text-text-meta">
          {{ tags.length }}개의 태그로 게시글을 탐색하세요
        </p>
      </header>

      <!-- 검색 및 정렬 -->
      <div class="mb-8 flex flex-col sm:flex-row gap-4">
        <!-- 검색 -->
        <div class="flex-1">
          <Input
            v-model="searchKeyword"
            placeholder="태그 검색..."
          />
        </div>

        <!-- 정렬 옵션 -->
        <div class="flex gap-2">
          <Button
            :variant="sortOption === 'popular' ? 'primary' : 'secondary'"
            size="md"
            @click="sortOption = 'popular'"
          >
            인기순
          </Button>
          <Button
            :variant="sortOption === 'name' ? 'primary' : 'secondary'"
            size="md"
            @click="sortOption = 'name'"
          >
            이름순
          </Button>
          <Button
            :variant="sortOption === 'latest' ? 'primary' : 'secondary'"
            size="md"
            @click="sortOption = 'latest'"
          >
            최신순
          </Button>
        </div>
      </div>

      <!-- Loading State -->
      <Card v-if="isLoading" class="text-center py-24 bg-bg-muted border-0 shadow-none">
        <Spinner size="lg" class="mx-auto mb-5" />
        <p class="text-text-meta text-lg">태그 목록을 불러오는 중...</p>
      </Card>

      <!-- Error State -->
      <Card v-else-if="error" class="bg-status-error-bg border-status-error/20 py-16 text-center">
        <div class="text-4xl text-status-error mb-4">❌</div>
        <div class="text-status-error font-semibold text-lg mb-2">{{ error }}</div>
        <Button variant="secondary" class="mt-4" @click="loadTags">
          다시 시도
        </Button>
      </Card>

      <!-- Empty State -->
      <Card v-else-if="sortedTags.length === 0" class="text-center py-20">
        <div class="text-6xl mb-4">🔍</div>
        <h3 class="text-2xl font-bold text-text-heading mb-2">
          {{ searchKeyword ? '검색 결과가 없습니다' : '태그가 없습니다' }}
        </h3>
        <p class="text-text-meta">
          {{ searchKeyword ? '다른 검색어를 시도해보세요.' : '아직 생성된 태그가 없습니다.' }}
        </p>
      </Card>

      <!-- Tag Cloud -->
      <div v-else>
        <!-- 그리드 뷰 (인기순, 최신순) -->
        <div
          v-if="sortOption !== 'name'"
          class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6"
        >
          <Card
            v-for="tag in sortedTags"
            :key="tag.id"
            hoverable
            @click="goToTag(tag.name)"
            class="cursor-pointer group"
          >
            <div class="p-6">
              <div class="flex items-start justify-between mb-3">
                <h3 :class="['font-bold group-hover:text-brand-primary transition-colors', getTagColor(tag.name)]">
                  #{{ tag.name }}
                </h3>
                <span class="px-3 py-1 bg-brand-primary/10 text-brand-primary text-sm rounded-full font-semibold">
                  {{ tag.postCount }}
                </span>
              </div>

              <p v-if="tag.description" class="text-text-meta text-sm mb-4 line-clamp-2">
                {{ tag.description }}
              </p>

              <div class="text-xs text-text-meta">
                마지막 사용: {{ formatDate(tag.lastUsedAt) }}
              </div>
            </div>
          </Card>
        </div>

        <!-- 태그 클라우드 (이름순) -->
        <Card v-else class="p-8">
          <div class="flex flex-wrap gap-6 justify-center items-center">
            <button
              v-for="tag in sortedTags"
              :key="tag.id"
              @click="goToTag(tag.name)"
              :class="[
                'transition-all hover:scale-110',
                getTagSize(tag.postCount),
                getTagColor(tag.name)
              ]"
            >
              #{{ tag.name }}
              <span class="text-sm text-text-meta ml-1">({{ tag.postCount }})</span>
            </button>
          </div>
        </Card>
      </div>

      <!-- 통계 요약 -->
      <Card v-if="!isLoading && !error && sortedTags.length > 0" class="mt-8 p-6 bg-bg-muted">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
          <div>
            <div class="text-2xl font-bold text-brand-primary">
              {{ sortedTags.length }}
            </div>
            <div class="text-text-meta text-sm">전체 태그</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">
              {{ sortedTags.reduce((sum, tag) => sum + tag.postCount, 0) }}
            </div>
            <div class="text-text-meta text-sm">전체 게시글</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">
              {{ Math.max(...sortedTags.map(t => t.postCount), 0) }}
            </div>
            <div class="text-text-meta text-sm">최다 게시글</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">
              {{ (sortedTags.reduce((sum, tag) => sum + tag.postCount, 0) / sortedTags.length).toFixed(1) }}
            </div>
            <div class="text-text-meta text-sm">평균 게시글</div>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
