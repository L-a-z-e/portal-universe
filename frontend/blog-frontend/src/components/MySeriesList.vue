<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMySeries, createSeries, updateSeries, deleteSeries } from '@/api/series'
import type { SeriesListResponse, SeriesCreateRequest, SeriesUpdateRequest } from '@/dto/series'
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
        <Button variant="primary" class="bg-status-error hover:bg-red-700" @click="handleDelete" :disabled="isSubmitting">
          {{ isSubmitting ? '삭제 중...' : '삭제' }}
        </Button>
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
  color: var(--color-text-heading);
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
  color: var(--color-text-heading);
  margin: 0 0 0.5rem;
}

.empty-desc {
  font-size: 0.875rem;
  color: var(--color-text-meta);
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
  color: var(--color-text-heading);
  margin: 0 0 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 0.875rem;
  color: var(--color-text-body);
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
  color: var(--color-text-meta);
}

.post-count {
  font-weight: 600;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border-default);
  background: var(--color-bg-muted);
}

.delete-btn {
  color: var(--color-status-error) !important;
  border-color: var(--color-status-error) !important;
}

.delete-btn:hover {
  background: var(--color-status-error-bg) !important;
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
  color: var(--color-status-error);
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
  color: var(--color-text-body);
  margin: 0 0 0.5rem;
}

.delete-note {
  color: var(--color-text-meta);
  font-size: 0.875rem;
  margin: 0;
}
</style>
