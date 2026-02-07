import { defineStore } from 'pinia';
import { ref } from 'vue';
import { searchPosts } from '../api/posts';
import type { PostSummaryResponse, PageResponse } from '@/types';

export const useSearchStore = defineStore('search', () => {
  // 상태 변수
  const keyword = ref('');
  const results = ref<PostSummaryResponse[]>([]);
  const isSearching = ref(false);
  const error = ref<string | null>(null);

  // 페이징
  const currentPage = ref(1);
  const totalPages = ref(0);
  const hasMore = ref(true);

  // 새 검색(키워드 변경)
  async function search(keywordParam: string) {
    keyword.value = keywordParam;
    results.value = [];
    currentPage.value = 1;
    totalPages.value = 0;
    hasMore.value = true;
    error.value = null;

    if (!keyword.value.trim()) return;

    isSearching.value = true;
    try {
      const res: PageResponse<PostSummaryResponse> = await searchPosts(keyword.value, 1, 10);
      results.value = res.items;
      currentPage.value = res.page;
      totalPages.value = res.totalPages;
      hasMore.value = res.page < res.totalPages;
    } catch (err) {
      error.value = '검색 결과를 불러올 수 없습니다.';
      results.value = [];
    } finally {
      isSearching.value = false;
    }
  }

  // 추가 페이지 로드
  async function loadMore() {
    if (!hasMore.value || isSearching.value) return;
    isSearching.value = true;
    try {
      const res: PageResponse<PostSummaryResponse> = await searchPosts(keyword.value, currentPage.value + 1, 10);
      results.value = [...results.value, ...res.items];
      currentPage.value = res.page;
      totalPages.value = res.totalPages;
      hasMore.value = res.page < res.totalPages;
    } catch (err) {
      error.value = '더 많은 검색 결과를 불러올 수 없습니다.';
    } finally {
      isSearching.value = false;
    }
  }

  // 초기화
  function clear() {
    keyword.value = '';
    results.value = [];
    currentPage.value = 1;
    totalPages.value = 0;
    hasMore.value = false;
    error.value = null;
    isSearching.value = false;
  }

  return {
    keyword,
    results,
    isSearching,
    error,
    currentPage,
    totalPages,
    hasMore,
    search,
    loadMore,
    clear,
  };
});