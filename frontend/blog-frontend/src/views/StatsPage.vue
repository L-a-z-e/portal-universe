<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Spinner, Button, Table } from '@portal/design-vue'
import type { TableColumn } from '@portal/design-core'
import { getBlogStats, getCategoryStats, getPopularTags, getAuthorStats } from '@/api/posts'
import type { BlogStats, CategoryStats, TagStatsResponse, AuthorStats } from '@/types'
import { usePortalAuth } from '@portal/vue-bridge'

// Composables
const { isAuthenticated, userUuid } = usePortalAuth()

// State
const loading = ref(false)
const error = ref<string | null>(null)
const blogStats = ref<BlogStats | null>(null)
const categoryStats = ref<CategoryStats[]>([])
const popularTags = ref<TagStatsResponse[]>([])
const authorStats = ref<AuthorStats | null>(null)

// Methods
const fetchBlogStats = async () => {
  try {
    blogStats.value = await getBlogStats()
  } catch (e) {
    console.error('Failed to fetch blog stats:', e)
  }
}

const fetchCategoryStats = async () => {
  try {
    categoryStats.value = await getCategoryStats()
  } catch (e) {
    console.error('Failed to fetch category stats:', e)
  }
}

const fetchPopularTags = async () => {
  try {
    const response = await getPopularTags(20)
    popularTags.value = response
  } catch (e) {
    console.error('Failed to fetch popular tags:', e)
  }
}

const fetchAuthorStats = async () => {
  if (!isAuthenticated.value || !userUuid.value) return

  try {
    authorStats.value = await getAuthorStats(userUuid.value)
  } catch (e) {
    console.error('Failed to fetch author stats:', e)
  }
}

const categoryColumns: TableColumn<CategoryStats>[] = [
  { key: 'categoryName', label: '카테고리' },
  { key: 'postCount', label: '게시글 수' },
  { key: 'latestPostDate', label: '최근 게시일' },
]

const fetchAllStats = async () => {
  loading.value = true
  error.value = null

  try {
    await Promise.all([
      fetchBlogStats(),
      fetchCategoryStats(),
      fetchPopularTags(),
      fetchAuthorStats()
    ])
  } catch (e) {
    error.value = '통계를 불러오는 중 오류가 발생했습니다.'
    console.error('Failed to fetch stats:', e)
  } finally {
    loading.value = false
  }
}

const formatNumber = (num: number) => {
  return num.toLocaleString('ko-KR')
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('ko-KR')
}

// Lifecycle
onMounted(() => {
  fetchAllStats()
})
</script>

<template>
  <div class="w-full min-h-screen">
    <div class="max-w-3xl mx-auto px-6 py-8">
      <!-- Header -->
      <header class="mb-8">
        <h1 class="text-2xl font-bold text-text-heading">블로그 통계</h1>
      </header>

      <!-- Loading -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-24">
        <Spinner size="lg" class="mb-4" />
        <p class="text-text-meta text-sm">통계를 불러오는 중...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="flex flex-col items-center justify-center py-24">
        <p class="text-status-error mb-4">{{ error }}</p>
        <Button variant="secondary" size="sm" @click="fetchAllStats">다시 시도</Button>
      </div>

      <!-- Stats Content -->
      <div v-else class="space-y-10">
        <!-- 전체 통계 -->
        <section v-if="blogStats">
          <h2 class="text-lg font-semibold text-text-heading mb-4">전체 통계</h2>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div class="p-4 rounded-lg bg-bg-elevated border border-border-default">
              <p class="text-xs text-text-meta mb-1">총 게시글</p>
              <p class="text-2xl font-bold text-text-heading">{{ formatNumber(blogStats.totalPosts) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-bg-elevated border border-border-default">
              <p class="text-xs text-text-meta mb-1">발행됨</p>
              <p class="text-2xl font-bold text-text-heading">{{ formatNumber(blogStats.publishedPosts) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-bg-elevated border border-border-default">
              <p class="text-xs text-text-meta mb-1">총 조회수</p>
              <p class="text-2xl font-bold text-text-heading">{{ formatNumber(blogStats.totalViews) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-bg-elevated border border-border-default">
              <p class="text-xs text-text-meta mb-1">총 좋아요</p>
              <p class="text-2xl font-bold text-text-heading">{{ formatNumber(blogStats.totalLikes) }}</p>
            </div>
          </div>
        </section>

        <!-- 내 통계 (로그인 시) -->
        <section v-if="isAuthenticated && authorStats">
          <h2 class="text-lg font-semibold text-text-heading mb-4">내 통계</h2>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div class="p-4 rounded-lg bg-brand-primary/5 border border-brand-primary/20">
              <p class="text-xs text-text-meta mb-1">내 게시글</p>
              <p class="text-2xl font-bold text-brand-primary">{{ formatNumber(authorStats.totalPosts) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-brand-primary/5 border border-brand-primary/20">
              <p class="text-xs text-text-meta mb-1">발행됨</p>
              <p class="text-2xl font-bold text-brand-primary">{{ formatNumber(authorStats.publishedPosts) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-brand-primary/5 border border-brand-primary/20">
              <p class="text-xs text-text-meta mb-1">내 조회수</p>
              <p class="text-2xl font-bold text-brand-primary">{{ formatNumber(authorStats.totalViews) }}</p>
            </div>
            <div class="p-4 rounded-lg bg-brand-primary/5 border border-brand-primary/20">
              <p class="text-xs text-text-meta mb-1">내 좋아요</p>
              <p class="text-2xl font-bold text-brand-primary">{{ formatNumber(authorStats.totalLikes) }}</p>
            </div>
          </div>
          <div class="flex gap-6 mt-3 text-xs text-text-meta">
            <span>첫 게시글: <strong class="text-text-body">{{ formatDate(authorStats.firstPostDate) }}</strong></span>
            <span>최근 게시글: <strong class="text-text-body">{{ formatDate(authorStats.lastPostDate) }}</strong></span>
          </div>
        </section>

        <!-- 카테고리 통계 -->
        <section v-if="categoryStats.length > 0">
          <h2 class="text-lg font-semibold text-text-heading mb-4">카테고리별 통계</h2>
          <Table :columns="categoryColumns" :data="categoryStats" hoverable>
            <template #cell-postCount="{ value }">
              <span class="font-semibold text-brand-primary">{{ formatNumber(value as number) }}</span>
            </template>
            <template #cell-latestPostDate="{ value }">
              {{ formatDate(value as string) }}
            </template>
          </Table>
        </section>

        <!-- 인기 태그 -->
        <section v-if="popularTags.length > 0">
          <h2 class="text-lg font-semibold text-text-heading mb-4">인기 태그</h2>
          <div class="flex flex-wrap gap-3 items-center">
            <span
              v-for="tag in popularTags"
              :key="tag.name"
              class="inline-flex items-baseline gap-1 cursor-default hover:text-brand-primary transition-colors"
              :style="{ fontSize: `${Math.min(1 + tag.postCount * 0.05, 1.8)}rem` }"
            >
              <span class="font-semibold text-text-heading">{{ tag.name }}</span>
              <span class="text-xs text-text-meta">({{ formatNumber(tag.postCount) }})</span>
            </span>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>
