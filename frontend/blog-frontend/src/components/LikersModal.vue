<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Avatar, Button, Modal, Spinner } from '@portal/design-vue';
import { getLikers } from '@/api/likes';
import type { LikerResponse, PageResponse } from '@/types';

interface Props {
  postId: string;
  isOpen: boolean;
}
const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
}>();

const router = useRouter();

const loading = ref(false);
const likers = ref<LikerResponse[]>([]);
const page = ref(1);
const hasMore = ref(false);

async function loadData() {
  loading.value = true;
  try {
    const response: PageResponse<LikerResponse> = await getLikers(props.postId, page.value, 20);

    if (page.value === 1) {
      likers.value = response.items;
    } else {
      likers.value = [...likers.value, ...response.items];
    }

    hasMore.value = response.page < response.totalPages;
  } catch (error) {
    console.error('Failed to load likers:', error);
  } finally {
    loading.value = false;
  }
}

function loadMore() {
  if (!hasMore.value || loading.value) return;
  page.value++;
  loadData();
}

function goToProfile(liker: LikerResponse) {
  if (liker.userName) {
    router.push(`/@${liker.userName}`);
    emit('close');
  }
}

watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    page.value = 1;
    likers.value = [];
    loadData();
  }
});
</script>

<template>
  <Modal
    :model-value="isOpen"
    title="좋아요한 사용자"
    size="md"
    @update:model-value="!$event && emit('close')"
    @close="emit('close')"
  >
    <div class="likers-modal-content">
      <!-- 로딩 -->
      <div v-if="loading && page === 1" class="loading-container">
        <Spinner size="lg" />
      </div>

      <!-- 빈 상태 -->
      <div v-else-if="likers.length === 0" class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
        <p>아직 좋아요한 사용자가 없습니다</p>
      </div>

      <!-- 사용자 목록 -->
      <div v-else class="user-list">
        <div
          v-for="liker in likers"
          :key="liker.userId"
          class="user-item"
          @click="goToProfile(liker)"
        >
          <div class="user-info">
            <Avatar
              :src="liker.profileImageUrl ?? undefined"
              :name="liker.nickname || 'User'"
              size="md"
            />
            <div class="user-details">
              <span class="username">{{ liker.nickname || 'Unknown' }}</span>
              <span class="liked-at">{{ new Date(liker.likedAt).toLocaleDateString('ko-KR') }}</span>
            </div>
          </div>
        </div>

        <!-- 더 보기 -->
        <div v-if="hasMore" class="load-more">
          <Button
            variant="outline"
            size="sm"
            :disabled="loading"
            @click="loadMore"
          >
            <Spinner v-if="loading" size="sm" />
            <span v-else>더 보기</span>
          </Button>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.likers-modal-content {
  max-height: 400px;
  overflow-y: auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  text-align: center;
  color: var(--semantic-text-meta);
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 0.75rem;
  border-radius: 0.5rem;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: var(--semantic-surface-alt);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 600;
  color: var(--semantic-text-heading);
}

.liked-at {
  font-size: 0.8125rem;
  color: var(--semantic-text-meta);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 1rem;
}
</style>
