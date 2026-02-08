<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMySeries, createSeries, updateSeries, deleteSeries, getSeriesPosts, addPostToSeries, removePostFromSeries } from '@/api/series'
import { getMyPosts } from '@/api/posts'
import type { SeriesListResponse, SeriesCreateRequest, SeriesUpdateRequest } from '@/dto/series'
import type { PostSummaryResponse } from '@/dto/post'
import { Button, Card, Input, Textarea, Modal } from '@portal/design-system-vue'

const router = useRouter()

// State
const loading = ref(false)
const seriesList = ref<SeriesListResponse[]>([])
const showModal = ref(false)
const showDeleteConfirm = ref(false)
const modalMode = ref<'create' | 'edit'>('create')
const currentSeries = ref<SeriesListResponse | null>(null)
const isSubmitting = ref(false)
const formData = ref({
  name: '',
  description: '',
  thumbnailUrl: ''
})
const formError = ref('')

// 포스트 관리 상태
const showPostsModal = ref(false)
const manageSeries = ref<SeriesListResponse | null>(null)
const seriesPosts = ref<PostSummaryResponse[]>([])
const myPosts = ref<PostSummaryResponse[]>([])
const postsLoading = ref(false)
const addingPostId = ref<string | null>(null)
const removingPostId = ref<string | null>(null)

const fetchSeries = async () => {
  loading.value = true
  try {
    seriesList.value = await getMySeries()
  } catch (error) {
    console.error('Failed to fetch series:', error)
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  modalMode.value = 'create'
  formData.value = { name: '', description: '', thumbnailUrl: '' }
  formError.value = ''
  showModal.value = true
}

const openEditModal = (series: SeriesListResponse) => {
  modalMode.value = 'edit'
  currentSeries.value = series
  formData.value = {
    name: series.name,
    description: series.description || '',
    thumbnailUrl: series.thumbnailUrl || ''
  }
  formError.value = ''
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  currentSeries.value = null
}

const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    formError.value = '시리즈 이름을 입력해주세요'
    return
  }

  isSubmitting.value = true
  try {
    if (modalMode.value === 'create') {
      const request: SeriesCreateRequest = {
        name: formData.value.name,
        description: formData.value.description || undefined,
        thumbnailUrl: formData.value.thumbnailUrl || undefined
      }
      await createSeries(request)
    } else if (currentSeries.value) {
      const request: SeriesUpdateRequest = {
        name: formData.value.name,
        description: formData.value.description || undefined,
        thumbnailUrl: formData.value.thumbnailUrl || undefined
      }
      await updateSeries(currentSeries.value.id, request)
    }

    closeModal()
    await fetchSeries()
  } catch (error) {
    console.error('Failed to save series:', error)
    formError.value = '시리즈 저장에 실패했습니다'
  } finally {
    isSubmitting.value = false
  }
}

const openDeleteConfirm = (series: SeriesListResponse) => {
  currentSeries.value = series
  showDeleteConfirm.value = true
}

const handleDelete = async () => {
  if (!currentSeries.value) return

  isSubmitting.value = true
  try {
    await deleteSeries(currentSeries.value.id)
    showDeleteConfirm.value = false
    currentSeries.value = null
    await fetchSeries()
  } catch (error) {
    console.error('Failed to delete series:', error)
  } finally {
    isSubmitting.value = false
  }
}

const goToSeries = (seriesId: string) => {
  router.push(`/series/${seriesId}`)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('ko-KR')
}

// 포스트 관리 모달 열기
const openPostsModal = async (series: SeriesListResponse) => {
  manageSeries.value = series
  showPostsModal.value = true
  postsLoading.value = true
  try {
    const [postsData, myPostsData] = await Promise.all([
      getSeriesPosts(series.id),
      getMyPosts('PUBLISHED', 1, 100)
    ])
    seriesPosts.value = postsData
    myPosts.value = myPostsData.items
  } catch (error) {
    console.error('Failed to load posts:', error)
  } finally {
    postsLoading.value = false
  }
}

const closePostsModal = () => {
  showPostsModal.value = false
  manageSeries.value = null
  seriesPosts.value = []
  myPosts.value = []
}

// 시리즈에 포함되지 않은 내 게시글 필터링
const availablePosts = () => {
  const seriesPostIds = new Set(seriesPosts.value.map(p => p.id))
  return myPosts.value.filter(p => !seriesPostIds.has(p.id))
}

// 포스트를 시리즈에 추가
const handleAddPost = async (postId: string) => {
  if (!manageSeries.value) return
  addingPostId.value = postId
  try {
    await addPostToSeries(manageSeries.value.id, postId)
    // 목록 갱신
    const addedPost = myPosts.value.find(p => p.id === postId)
    if (addedPost) {
      seriesPosts.value.push(addedPost)
    }
    await fetchSeries()
  } catch (error) {
    console.error('Failed to add post to series:', error)
  } finally {
    addingPostId.value = null
  }
}

// 시리즈에서 포스트 제거
const handleRemovePost = async (postId: string) => {
  if (!manageSeries.value) return
  removingPostId.value = postId
  try {
    await removePostFromSeries(manageSeries.value.id, postId)
    seriesPosts.value = seriesPosts.value.filter(p => p.id !== postId)
    await fetchSeries()
  } catch (error) {
    console.error('Failed to remove post from series:', error)
  } finally {
    removingPostId.value = null
  }
}

onMounted(() => fetchSeries())
</script>

<template>
  <div class="my-series">
    <!-- Header -->
    <div class="series-header">
      <h2 class="series-title">내 시리즈</h2>
      <Button variant="primary" size="sm" @click="openCreateModal">
        새 시리즈 만들기
      </Button>
    </div>

    <!-- Loading -->
    <div v-if="loading && seriesList.length === 0" class="loading-state">
      <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- Empty State -->
    <div v-else-if="seriesList.length === 0" class="empty-state">
      <div class="empty-icon">📚</div>
      <p class="empty-title">아직 시리즈가 없습니다</p>
      <p class="empty-desc">관련 게시글을 묶어 시리즈를 만들어보세요</p>
      <Button variant="primary" @click="openCreateModal">
        첫 시리즈 만들기
      </Button>
    </div>

    <!-- Series Grid -->
    <div v-else class="series-grid">
      <Card
        v-for="series in seriesList"
        :key="series.id"
        class="series-card"
      >
        <div class="card-body" @click="goToSeries(series.id)">
          <h3 class="card-name">{{ series.name }}</h3>
          <p v-if="series.description" class="card-desc">{{ series.description }}</p>
          <div class="card-meta">
            <span class="post-count">게시글 {{ series.postCount }}개</span>
            <span class="updated-at">{{ formatDate(series.updatedAt) }}</span>
          </div>
        </div>
        <div class="card-actions">
          <Button variant="outline" size="sm" @click.stop="openPostsModal(series)">게시글 관리</Button>
          <Button variant="outline" size="sm" @click.stop="openEditModal(series)">수정</Button>
          <Button variant="outline" size="sm" class="delete-btn" @click.stop="openDeleteConfirm(series)">삭제</Button>
        </div>
      </Card>
    </div>

    <!-- Create/Edit Modal -->
    <Modal
      :model-value="showModal"
      :title="modalMode === 'create' ? '새 시리즈 만들기' : '시리즈 수정'"
      size="md"
      @update:model-value="showModal = $event"
      @close="closeModal"
    >
      <div class="modal-form">
        <div class="form-field">
          <Input
            v-model="formData.name"
            label="시리즈 이름 *"
            placeholder="시리즈 이름을 입력하세요"
          />
        </div>
        <div class="form-field">
          <Textarea
            v-model="formData.description"
            label="설명"
            placeholder="시리즈에 대한 설명을 입력하세요"
            :rows="3"
          />
        </div>
        <div class="form-field">
          <Input
            v-model="formData.thumbnailUrl"
            label="썸네일 URL"
            placeholder="https://example.com/image.jpg"
          />
        </div>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <div class="modal-actions">
        <Button variant="secondary" @click="closeModal" :disabled="isSubmitting">취소</Button>
        <Button variant="primary" @click="handleSubmit" :disabled="isSubmitting">
          {{ isSubmitting ? '저장 중...' : (modalMode === 'create' ? '만들기' : '수정') }}
        </Button>
      </div>
    </Modal>

    <!-- Delete Confirm Modal -->
    <Modal
      :model-value="showDeleteConfirm"
      title="시리즈 삭제"
      size="sm"
      @update:model-value="showDeleteConfirm = $event"
      @close="showDeleteConfirm = false"
    >
      <p class="delete-message">
        <strong>{{ currentSeries?.name }}</strong> 시리즈를 삭제하시겠습니까?
      </p>
      <p class="delete-note">시리즈를 삭제해도 포함된 게시글은 삭제되지 않습니다.</p>
      <div class="modal-actions">
        <Button variant="secondary" @click="showDeleteConfirm = false" :disabled="isSubmitting">취소</Button>
        <Button variant="danger" @click="handleDelete" :disabled="isSubmitting">
          {{ isSubmitting ? '삭제 중...' : '삭제' }}
        </Button>
      </div>
    </Modal>

    <!-- Posts Management Modal -->
    <Modal
      :model-value="showPostsModal"
      :title="`게시글 관리 - ${manageSeries?.name || ''}`"
      size="lg"
      @update:model-value="showPostsModal = $event"
      @close="closePostsModal"
    >
      <div v-if="postsLoading" class="posts-loading">
        <div class="w-8 h-8 border-4 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
      </div>

      <div v-else class="posts-manage">
        <!-- 현재 시리즈에 포함된 게시글 -->
        <div class="posts-section">
          <h4 class="section-title">시리즈에 포함된 게시글 ({{ seriesPosts.length }}개)</h4>
          <div v-if="seriesPosts.length === 0" class="section-empty">
            아직 게시글이 없습니다. 아래에서 추가해주세요.
          </div>
          <div v-else class="posts-list">
            <div v-for="(post, index) in seriesPosts" :key="post.id" class="post-item">
              <div class="post-order">{{ index + 1 }}</div>
              <div class="post-info">
                <span class="post-title">{{ post.title }}</span>
                <span class="post-date">{{ formatDate(post.publishedAt) }}</span>
              </div>
              <Button
                variant="outline"
                size="sm"
                class="remove-btn"
                :disabled="removingPostId === post.id"
                @click="handleRemovePost(post.id)"
              >
                {{ removingPostId === post.id ? '제거 중...' : '제거' }}
              </Button>
            </div>
          </div>
        </div>

        <!-- 추가 가능한 게시글 -->
        <div class="posts-section">
          <h4 class="section-title">추가 가능한 게시글</h4>
          <div v-if="availablePosts().length === 0" class="section-empty">
            추가할 수 있는 게시글이 없습니다.
          </div>
          <div v-else class="posts-list">
            <div v-for="post in availablePosts()" :key="post.id" class="post-item">
              <div class="post-info">
                <span class="post-title">{{ post.title }}</span>
                <span class="post-date">{{ formatDate(post.publishedAt) }}</span>
              </div>
              <Button
                variant="primary"
                size="sm"
                :disabled="addingPostId === post.id"
                @click="handleAddPost(post.id)"
              >
                {{ addingPostId === post.id ? '추가 중...' : '추가' }}
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal-actions">
        <Button variant="secondary" @click="closePostsModal">닫기</Button>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.my-series {
  width: 100%;
}

.series-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.series-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 4rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4rem 1rem;
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  margin: 0 0 0.5rem;
}

.empty-desc {
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
  margin: 0 0 1.5rem;
}

.series-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.series-card {
  transition: box-shadow 0.2s, transform 0.2s;
}

.series-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-body {
  padding: 1.25rem;
  cursor: pointer;
}

.card-name {
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0 0 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 0.875rem;
  color: var(--semantic-text-body);
  margin: 0 0 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
}

.post-count {
  font-weight: 600;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--semantic-border-default);
  background: var(--semantic-bg-muted);
}

.delete-btn {
  color: var(--semantic-status-error) !important;
  border-color: var(--semantic-status-error) !important;
}

.delete-btn:hover {
  background: var(--semantic-status-error-bg) !important;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  width: 100%;
}

.form-error {
  color: var(--semantic-status-error);
  font-size: 0.875rem;
  margin: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.delete-message {
  color: var(--semantic-text-body);
  margin: 0 0 0.5rem;
}

.delete-note {
  color: var(--semantic-text-meta);
  font-size: 0.875rem;
  margin: 0;
}

/* Posts Management Modal */
.posts-loading {
  display: flex;
  justify-content: center;
  padding: 3rem;
}

.posts-manage {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.posts-section {
  border: 1px solid var(--semantic-border-default);
  border-radius: 0.5rem;
  overflow: hidden;
}

.section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--semantic-text-heading);
  padding: 0.75rem 1rem;
  background: var(--semantic-bg-muted);
  border-bottom: 1px solid var(--semantic-border-default);
  margin: 0;
}

.section-empty {
  padding: 1.5rem 1rem;
  text-align: center;
  color: var(--semantic-text-meta);
  font-size: 0.875rem;
}

.posts-list {
  max-height: 300px;
  overflow-y: auto;
}

.post-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--semantic-border-muted);
}

.post-item:last-child {
  border-bottom: none;
}

.post-order {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--semantic-brand-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
}

.post-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.post-title {
  font-size: 0.875rem;
  color: var(--semantic-text-body);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-date {
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
}

.remove-btn {
  color: var(--semantic-status-error) !important;
  border-color: var(--semantic-status-error) !important;
  flex-shrink: 0;
}

.remove-btn:hover {
  background: var(--semantic-status-error-bg) !important;
}
</style>
