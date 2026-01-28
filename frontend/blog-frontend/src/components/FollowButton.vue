<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Button } from '@portal/design-system-vue';
import { useFollowStore } from '@/stores/followStore';

interface Props {
  username: string;
  targetUuid: string;
  initialFollowing?: boolean;
  size?: 'sm' | 'md' | 'lg';
  showText?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  initialFollowing: false,
  size: 'md',
  showText: true
});

const emit = defineEmits<{
  followChanged: [following: boolean, followerCount: number, followingCount: number];
}>();

const followStore = useFollowStore();

// State
const following = ref(props.initialFollowing);
const loading = ref(false);
const error = ref<string | null>(null);

// 팔로우 상태 확인
const isFollowing = computed(() => {
  return followStore.isFollowing(props.targetUuid) || following.value;
});

// 팔로우 토글
async function handleToggle() {
  if (loading.value) return;

  // Optimistic UI update
  const previousFollowing = following.value;
  following.value = !following.value;

  loading.value = true;
  error.value = null;

  try {
    const response = await followStore.toggleFollow(props.username, props.targetUuid);
    following.value = response.following;
    emit('followChanged', response.following, response.followerCount, response.followingCount);
  } catch (err: any) {
    console.error('Failed to toggle follow:', err);

    // 롤백
    following.value = previousFollowing;

    // 에러 메시지 설정
    if (err.response?.status === 401) {
      error.value = '로그인이 필요합니다';
    } else if (err.response?.status === 400) {
      error.value = '자기 자신을 팔로우할 수 없습니다';
    } else {
      error.value = '처리 중 오류가 발생했습니다';
    }

    // 에러 메시지 자동 숨김
    setTimeout(() => {
      error.value = null;
    }, 3000);
  } finally {
    loading.value = false;
  }
}

// 버튼 클래스
const buttonClass = computed(() => ({
  'follow-button': true,
  'following': isFollowing.value,
  'loading': loading.value
}));

onMounted(async () => {
  // 초기 상태가 제공되지 않은 경우 store에서 확인
  if (!props.initialFollowing) {
    try {
      const status = await followStore.checkFollowStatus(props.username);
      following.value = status;
    } catch (err) {
      console.error('Failed to check follow status:', err);
    }
  }
});
</script>

<template>
  <div class="follow-button-wrapper">
    <Button
      :class="buttonClass"
      :variant="isFollowing ? 'outline' : 'primary'"
      :size="size"
      :disabled="loading"
      @click="handleToggle"
    >
      <!-- Follow Icon -->
      <svg
        v-if="!isFollowing"
        class="follow-icon"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"
        />
      </svg>
      <!-- Following (check) Icon -->
      <svg
        v-else
        class="follow-icon"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M5 13l4 4L19 7"
        />
      </svg>

      <!-- Button Text -->
      <span v-if="showText" class="button-text">
        {{ isFollowing ? '팔로잉' : '팔로우' }}
      </span>
    </Button>

    <!-- Error Message -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<style scoped>
.follow-button-wrapper {
  position: relative;
  display: inline-block;
}

.follow-button {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  transition: all 0.2s ease;
  min-width: 90px;
}

.follow-button:hover:not(:disabled) {
  transform: scale(1.02);
}

.follow-button.following {
  color: var(--semantic-brand-primary);
  border-color: var(--semantic-brand-primary);
}

.follow-button.following:hover:not(:disabled) {
  background: var(--semantic-status-error-bg);
  border-color: var(--semantic-status-error);
  color: var(--semantic-status-error);
}

.follow-button.following:hover:not(:disabled) .button-text::after {
  content: '취소';
}

.follow-button.following:hover:not(:disabled) .button-text {
  visibility: hidden;
  position: relative;
}

.follow-button.following:hover:not(:disabled) .button-text::after {
  visibility: visible;
  position: absolute;
  left: 0;
}

.follow-button.loading {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Follow Icon */
.follow-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

/* Button Text */
.button-text {
  font-weight: 500;
  font-size: 0.875rem;
}

/* Error Message */
.error-message {
  position: absolute;
  top: calc(100% + 0.5rem);
  left: 50%;
  transform: translateX(-50%);
  background: var(--semantic-status-error-bg);
  color: var(--semantic-status-error);
  padding: 0.5rem 0.75rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: fadeIn 0.2s ease;
  z-index: 10;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-0.5rem);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

/* 반응형 */
@media (max-width: 640px) {
  .follow-button {
    min-width: 80px;
  }

  .follow-icon {
    width: 0.875rem;
    height: 0.875rem;
  }

  .button-text {
    font-size: 0.8125rem;
  }
}
</style>
