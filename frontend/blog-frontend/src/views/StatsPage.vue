<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, Spinner, Button } from '@portal/design-system-vue'
import { getBlogStats, getCategoryStats, getPopularTags, getAuthorStats } from '@/api/posts'
import type { BlogStats, CategoryStats, TagStatsResponse, AuthorStats } from '@/types'
import { usePortalAuth } from '@/composables/usePortalAuth'

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
  <div class="stats-page">
    <div class="stats-header">
      <h1 class="stats-title">블로그 통계</h1>
    </div>

    <div v-if="loading" class="loading-container">
      <Spinner size="lg" />
      <p class="loading-text">통계를 불러오는 중...</p>
    </div>

    <div v-else-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <Button variant="secondary" @click="fetchAllStats">다시 시도</Button>
    </div>

    <div v-else class="stats-content">
      <!-- 전체 통계 -->
      <section v-if="blogStats" class="stats-section">
        <h2 class="section-title">전체 통계</h2>
        <div class="stats-grid">
          <Card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">📝</div>
              <div class="stat-info">
                <p class="stat-label">총 게시글</p>
                <p class="stat-value">{{ formatNumber(blogStats.totalPosts) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">✅</div>
              <div class="stat-info">
                <p class="stat-label">발행된 게시글</p>
                <p class="stat-value">{{ formatNumber(blogStats.publishedPosts) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">👁️</div>
              <div class="stat-info">
                <p class="stat-label">총 조회수</p>
                <p class="stat-value">{{ formatNumber(blogStats.totalViews) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card">
            <div class="stat-content">
              <div class="stat-icon">❤️</div>
              <div class="stat-info">
                <p class="stat-label">총 좋아요</p>
                <p class="stat-value">{{ formatNumber(blogStats.totalLikes) }}</p>
              </div>
            </div>
          </Card>
        </div>
      </section>

      <!-- 내 통계 (로그인 시) -->
      <section v-if="isAuthenticated && authorStats" class="stats-section">
        <h2 class="section-title">내 통계</h2>
        <div class="stats-grid">
          <Card class="stat-card author-stat-card">
            <div class="stat-content">
              <div class="stat-icon">📄</div>
              <div class="stat-info">
                <p class="stat-label">내 게시글</p>
                <p class="stat-value">{{ formatNumber(authorStats.totalPosts) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card author-stat-card">
            <div class="stat-content">
              <div class="stat-icon">✨</div>
              <div class="stat-info">
                <p class="stat-label">발행된 게시글</p>
                <p class="stat-value">{{ formatNumber(authorStats.publishedPosts) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card author-stat-card">
            <div class="stat-content">
              <div class="stat-icon">👀</div>
              <div class="stat-info">
                <p class="stat-label">내 총 조회수</p>
                <p class="stat-value">{{ formatNumber(authorStats.totalViews) }}</p>
              </div>
            </div>
          </Card>

          <Card class="stat-card author-stat-card">
            <div class="stat-content">
              <div class="stat-icon">💖</div>
              <div class="stat-info">
                <p class="stat-label">내 총 좋아요</p>
                <p class="stat-value">{{ formatNumber(authorStats.totalLikes) }}</p>
              </div>
            </div>
          </Card>
        </div>

        <div class="author-dates">
          <p class="date-info">
            첫 게시글: <span class="date-value">{{ formatDate(authorStats.firstPostDate) }}</span>
          </p>
          <p class="date-info">
            최근 게시글: <span class="date-value">{{ formatDate(authorStats.lastPostDate) }}</span>
          </p>
        </div>
      </section>

      <!-- 카테고리 통계 -->
      <section v-if="categoryStats.length > 0" class="stats-section">
        <h2 class="section-title">카테고리별 통계</h2>
        <Card class="table-card">
          <div class="table-container">
            <table class="stats-table">
              <thead>
                <tr>
                  <th>카테고리</th>
                  <th>게시글 수</th>
                  <th>최근 게시일</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="category in categoryStats" :key="category.categoryName">
                  <td class="category-name">{{ category.categoryName }}</td>
                  <td class="post-count">{{ formatNumber(category.postCount) }}</td>
                  <td class="post-date">{{ formatDate(category.latestPostDate) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </Card>
      </section>

      <!-- 인기 태그 -->
      <section v-if="popularTags.length > 0" class="stats-section">
        <h2 class="section-title">인기 태그</h2>
        <Card class="tags-card">
          <div class="tags-container">
            <div
              v-for="tag in popularTags"
              :key="tag.name"
              class="tag-item"
              :style="{ fontSize: `${Math.min(1 + tag.postCount * 0.05, 1.8)}rem` }"
            >
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-count">({{ formatNumber(tag.postCount) }})</span>
            </div>
          </div>
        </Card>
      </section>
    </div>
  </div>
</template>

<style scoped>
.stats-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.stats-header {
  margin-bottom: 2rem;
}

.stats-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0;
}

/* Loading & Error */
.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
}

.loading-text {
  margin-top: 1rem;
  color: var(--semantic-text-meta);
  font-size: 0.95rem;
}

.error-message {
  color: var(--semantic-status-error);
  font-size: 1rem;
  margin-bottom: 1rem;
}

.retry-button {
  padding: 0.5rem 1.5rem;
  background-color: var(--semantic-brand-primary);
  color: white;
  border: none;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.95rem;
  transition: opacity 0.2s;
}

.retry-button:hover {
  opacity: 0.9;
}

/* Stats Content */
.stats-content {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.stats-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.stat-card {
  padding: 1.5rem;
  background-color: var(--semantic-bg-card);
  border: 1px solid var(--semantic-border-default);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.author-stat-card {
  background-color: var(--semantic-bg-elevated);
  border-color: var(--semantic-brand-primary);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  font-size: 2rem;
  line-height: 1;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
  margin: 0 0 0.25rem 0;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0;
}

.author-dates {
  display: flex;
  gap: 2rem;
  margin-top: 0.5rem;
  padding: 1rem;
  background-color: var(--semantic-bg-muted);
  border-radius: 0.5rem;
}

.date-info {
  font-size: 0.9rem;
  color: var(--semantic-text-meta);
  margin: 0;
}

.date-value {
  font-weight: 600;
  color: var(--semantic-text-body);
}

/* Table */
.table-card {
  padding: 0;
  overflow: hidden;
}

.table-container {
  overflow-x: auto;
}

.stats-table {
  width: 100%;
  border-collapse: collapse;
}

.stats-table thead {
  background-color: var(--semantic-bg-muted);
}

.stats-table th {
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: var(--semantic-text-heading);
  font-size: 0.95rem;
  border-bottom: 2px solid var(--semantic-border-default);
}

.stats-table td {
  padding: 1rem;
  color: var(--semantic-text-body);
  border-bottom: 1px solid var(--semantic-border-default);
}

.stats-table tbody tr:last-child td {
  border-bottom: none;
}

.stats-table tbody tr:hover {
  background-color: var(--semantic-bg-muted);
}

.category-name {
  font-weight: 500;
  color: var(--semantic-text-heading);
}

.post-count {
  font-weight: 600;
  color: var(--semantic-brand-primary);
}

.post-date {
  color: var(--semantic-text-meta);
  font-size: 0.9rem;
}

/* Tags */
.tags-card {
  padding: 2rem;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: center;
  justify-content: center;
}

.tag-item {
  display: inline-flex;
  align-items: baseline;
  gap: 0.25rem;
  cursor: default;
  transition: color 0.2s;
}

.tag-item:hover {
  color: var(--semantic-brand-primary);
}

.tag-name {
  font-weight: 600;
  color: var(--semantic-text-heading);
}

.tag-count {
  font-size: 0.85em;
  color: var(--semantic-text-meta);
}

/* Responsive */
@media (max-width: 768px) {
  .stats-page {
    padding: 1.5rem 1rem;
  }

  .stats-title {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.25rem;
  }

  .stats-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
  }

  .stat-card {
    padding: 1rem;
  }

  .stat-value {
    font-size: 1.5rem;
  }

  .author-dates {
    flex-direction: column;
    gap: 0.5rem;
  }

  .stats-table th,
  .stats-table td {
    padding: 0.75rem 0.5rem;
    font-size: 0.9rem;
  }

  .tags-container {
    gap: 1rem;
  }

  .tag-item {
    font-size: 0.9rem !important;
  }
}
</style>
