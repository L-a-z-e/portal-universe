<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Avatar, Button, Modal, Spinner } from '@portal/design-vue';
import { useFollowStore } from '@/stores/followStore';
import FollowButton from './FollowButton.vue';
import type { FollowUserResponse, FollowListResponse } from '@/dto/follow';

interface Props {
  username: string;
  isOpen: boolean;
  type: 'followers' | 'following';
}
const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
}>();

const router = useRouter();
const followStore = useFollowStore();

// State
const loading = ref(false);
const users = ref<FollowUserResponse[]>([]);
const page = ref(0);
const hasNext = ref(false);
const totalElements = ref(0);

// Modal title
const title = computed(() => {
  return props.type === 'followers' ? '팔로워' : '팔로잉';
});

// 데이터 로드
async function loadData() {
  loading.value = true;
  try {
    let response: FollowListResponse;
    if (props.type === 'followers') {
      response = await followStore.getFollowers(props.username, page.value, 20);
    } else {
      response = await followStore.getFollowings(props.username, page.value, 20);
    }

    if (page.value === 0) {
      users.value = response.users;
    } else {
      users.value = [...users.value, ...response.users];
    }

    hasNext.value = response.hasNext;
    totalElements.value = response.totalElements;
  } catch (error) {
    console.error('Failed to load follow list:', error);
  } finally {
    loading.value = false;
  }
}

// 더 보기
function loadMore() {
  if (!hasNext.value || loading.value) return;
  page.value++;
  loadData();
}

// 사용자 프로필로 이동
function goToProfile(user: FollowUserResponse) {
  if (user.username) {
    router.push(`/@${user.username}`);
    emit('close');
  }
}

// 모달 열릴 때 데이터 로드
watch(() => props.isOpen, (isOpen) => {
  if (isOpen) {
    page.value = 0;
    users.value = [];
    loadData();
  }
});

// type이 바뀔 때도 리로드
watch(() => props.type, () => {
  if (props.isOpen) {
    page.value = 0;
    users.value = [];
    loadData();
  }
});
</script>

<template>
  <Modal
    :model-value="isOpen"
    :title="title"
    size="md"
    @update:model-value="!$event && emit('close')"
    @close="emit('close')"
  >
    <div class="follow-modal-content">
      <!-- 로딩 -->
      <div v-if="loading && page === 0" class="loading-container">
        <Spinner size="lg" />
      </div>

      <!-- 빈 상태 -->
      <div v-else-if="users.length === 0" class="empty-state">
        <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
          />
        </svg>
        <p>{{ type === 'followers' ? '아직 팔로워가 없습니다' : '아직 팔로우하는 사용자가 없습니다' }}</p>
      </div>

      <!-- 사용자 목록 -->
      <div v-else class="user-list">
        <div
          v-for="user in users"
          :key="user.uuid"
          class="user-item"
        >
          <div
            class="user-info"
            @click="goToProfile(user)"
          >
            <Avatar
              :src="user.profileImageUrl ?? undefined"
              :name="user.nickname || user.username || 'User'"
              size="md"
            />
            <div class="user-details">
              <span class="nickname">{{ user.nickname }}</span>
              <span v-if="user.username" class="username">@{{ user.username }}</span>
              <span v-if="user.bio" class="bio">{{ user.bio }}</span>
            </div>
          </div>

          <!-- 팔로우 버튼 (자기 자신 제외) -->
          <FollowButton
            v-if="user.username"
            :username="user.username"
            :target-uuid="user.uuid"
            size="sm"
            :show-text="false"
            class="follow-btn"
          />
        </div>

        <!-- 더 보기 버튼 -->
        <div v-if="hasNext" class="load-more">
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
.follow-modal-content {
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
  justify-content: space-between;
  padding: 0.75rem;
  border-radius: 0.5rem;
  transition: background 0.2s;
}

.user-item:hover {
  background: var(--semantic-surface-alt);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.nickname {
  font-weight: 600;
  color: var(--semantic-text-heading);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.username {
  font-size: 0.875rem;
  color: var(--semantic-text-meta);
}

.bio {
  font-size: 0.8125rem;
  color: var(--semantic-text-body);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.follow-btn {
  flex-shrink: 0;
  margin-left: 0.5rem;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 1rem;
}
</style>
