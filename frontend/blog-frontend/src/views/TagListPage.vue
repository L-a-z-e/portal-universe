<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getAllTags } from '../api/tags';
import type { TagResponse } from '@/types';
import { Input, Spinner, Button } from '@portal/design-vue';

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

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    filtered = filtered.filter(tag =>
      tag.name.toLowerCase().includes(keyword) ||
      tag.description?.toLowerCase().includes(keyword)
    );
  }

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

// 태그 크기 계산 (postCount 기반, 이름순 클라우드용)
const getTagSize = (postCount: number): string => {
  const maxCount = Math.max(...tags.value.map(t => t.postCount), 1);
  const ratio = postCount / maxCount;

  if (ratio >= 0.8) return 'text-4xl font-bold';
  if (ratio >= 0.6) return 'text-3xl font-semibold';
  if (ratio >= 0.4) return 'text-2xl font-semibold';
  if (ratio >= 0.2) return 'text-xl font-medium';
  return 'text-lg';
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
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Header -->
      <header class="mb-8">
        <h1 class="text-2xl font-bold text-text-heading mb-1">태그</h1>
        <p class="text-sm text-text-meta">
          {{ tags.length }}개의 태그로 게시글을 탐색하세요
        </p>
      </header>

      <!-- 검색 및 정렬 -->
      <div class="mb-8 flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <Input
            v-model="searchKeyword"
            placeholder="태그 검색..."
          />
        </div>

        <div class="flex gap-2">
          <button
            v-for="opt in ([
              { label: '인기순', value: 'popular' as const },
              { label: '이름순', value: 'name' as const },
              { label: '최신순', value: 'latest' as const },
            ])"
            :key="opt.value"
            @click="sortOption = opt.value"
            class="px-3 py-1.5 text-xs font-medium rounded-full transition-colors"
            :class="sortOption === opt.value
              ? 'bg-brand-primary text-white'
              : 'bg-bg-muted text-text-meta hover:bg-bg-hover hover:text-text-body'"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="isLoading" class="flex justify-center py-24">
        <div class="text-center">
          <Spinner size="lg" class="mx-auto mb-4" />
          <p class="text-text-meta">태그 목록을 불러오는 중...</p>
        </div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="text-center py-16">
        <div class="text-status-error font-semibold mb-2">{{ error }}</div>
        <Button variant="ghost" size="sm" @click="loadTags" class="mt-2">
          다시 시도
        </Button>
      </div>

      <!-- Empty -->
      <div v-else-if="sortedTags.length === 0" class="text-center py-20">
        <h3 class="text-lg font-semibold text-text-heading mb-2">
          {{ searchKeyword ? '검색 결과가 없습니다' : '태그가 없습니다' }}
        </h3>
        <p class="text-text-meta text-sm">
          {{ searchKeyword ? '다른 검색어를 시도해보세요.' : '아직 생성된 태그가 없습니다.' }}
        </p>
      </div>

      <!-- Tag List -->
      <div v-else>
        <!-- 리스트 뷰 (인기순, 최신순) -->
        <div v-if="sortOption !== 'name'" class="space-y-0">
          <button
            v-for="tag in sortedTags"
            :key="tag.id"
            @click="goToTag(tag.name)"
            class="w-full flex items-center justify-between py-4 border-b border-border-default hover:bg-bg-hover transition-colors text-left group"
          >
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span class="font-semibold text-text-heading group-hover:text-brand-primary transition-colors">#{{ tag.name }}</span>
                <span class="px-2 py-0.5 bg-brand-primary/10 text-brand-primary text-xs rounded-full font-medium">
                  {{ tag.postCount }}
                </span>
              </div>
              <p v-if="tag.description" class="text-sm text-text-meta line-clamp-1">
                {{ tag.description }}
              </p>
            </div>
            <span class="text-xs text-text-meta ml-4 flex-shrink-0">
              {{ formatDate(tag.lastUsedAt) }}
            </span>
          </button>
        </div>

        <!-- 태그 클라우드 (이름순) -->
        <div v-else class="flex flex-wrap gap-4 justify-center items-center py-8">
          <button
            v-for="tag in sortedTags"
            :key="tag.id"
            @click="goToTag(tag.name)"
            :class="[
              'transition-all hover:text-brand-primary text-text-heading',
              getTagSize(tag.postCount),
            ]"
          >
            #{{ tag.name }}
            <span class="text-sm text-text-meta ml-1">({{ tag.postCount }})</span>
          </button>
        </div>
      </div>

      <!-- 통계 요약 -->
      <div v-if="!isLoading && !error && sortedTags.length > 0" class="mt-8 py-6 border-t border-border-default">
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-center">
          <div>
            <div class="text-2xl font-bold text-brand-primary">{{ sortedTags.length }}</div>
            <div class="text-text-meta text-xs">전체 태그</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">{{ sortedTags.reduce((sum, tag) => sum + tag.postCount, 0) }}</div>
            <div class="text-text-meta text-xs">전체 게시글</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">{{ Math.max(...sortedTags.map(t => t.postCount), 0) }}</div>
            <div class="text-text-meta text-xs">최다 게시글</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-brand-primary">{{ (sortedTags.reduce((sum, tag) => sum + tag.postCount, 0) / sortedTags.length).toFixed(1) }}</div>
            <div class="text-text-meta text-xs">평균 게시글</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
