<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Button, useApiError } from '@portal/design-system-vue';
import { toggleLike, getLikeStatus } from '@/api/likes';

interface Props {
  postId: string;
  initialLiked?: boolean;
  initialCount?: number;
}
const props = withDefaults(defineProps<Props>(), {
  initialLiked: false,
  initialCount: 0
});

const emit = defineEmits<{
  likeChanged: [liked: boolean, count: number];
}>();

const { getErrorMessage } = useApiError();

// State
const liked = ref(props.initialLiked);
const likeCount = ref(props.initialCount);
const loading = ref(false);
const error = ref<string | null>(null);

// 좋아요 버튼 애니메이션
const animating = ref(false);

// 좋아요 상태 확인
async function fetchLikeStatus() {
  try {
    const status = await getLikeStatus(props.postId);
    liked.value = status.liked;
    likeCount.value = status.likeCount;
  } catch (err) {
    console.error('Failed to fetch like status:', err);
  }
}

// 좋아요 토글
async function handleToggle() {
  if (loading.value) return;

  // Optimistic UI update
  const previousLiked = liked.value;
  const previousCount = likeCount.value;

  liked.value = !liked.value;
  likeCount.value = liked.value ? likeCount.value + 1 : likeCount.value - 1;

  // 애니메이션 트리거
  if (liked.value) {
    animating.value = true;
    setTimeout(() => {
      animating.value = false;
    }, 600);
  }

  // API 호출
  loading.value = true;
  error.value = null;

  try {
    const response = await toggleLike(props.postId);
    liked.value = response.liked;
    likeCount.value = response.likeCount;
    emit('likeChanged', response.liked, response.likeCount);
  } catch (err) {
    console.error('Failed to toggle like:', err);

    // 롤백
    liked.value = previousLiked;
    likeCount.value = previousCount;

    // 에러 메시지 설정
    error.value = getErrorMessage(err, '좋아요 처리 중 오류가 발생했습니다.');

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
  'like-button': true,
  'liked': liked.value,
  'animating': animating.value,
  'loading': loading.value
}));

onMounted(() => {
  // 초기 상태가 제공되지 않은 경우 API에서 가져오기
  if (!props.initialLiked && !props.initialCount) {
    fetchLikeStatus();
  }
});
</script>

<template>
  <div class="like-button-wrapper">
    <Button
      :class="buttonClass"
      :variant="liked ? 'primary' : 'outline'"
      :disabled="loading"
      @click="handleToggle"
      size="md"
    >
      <!-- Heart Icon -->
      <svg
        class="heart-icon"
        :class="{ filled: liked }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          v-if="!liked"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
        />
        <path
          v-else
          fill="currentColor"
          d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"
        />
      </svg>

      <!-- Like Count -->
      <span class="like-count">{{ likeCount }}</span>
    </Button>

    <!-- Error Message -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<style scoped>
.like-button-wrapper {
  position: relative;
  display: inline-block;
}

.like-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s ease;
  min-width: 100px;
}

.like-button:hover:not(:disabled) {
  transform: scale(1.05);
}

.like-button.liked {
  background: var(--semantic-brand-primary);
  color: white;
  border-color: var(--semantic-brand-primary);
}

.like-button.loading {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Heart Icon */
.heart-icon {
  width: 1.25rem;
  height: 1.25rem;
  transition: all 0.3s ease;
}

.heart-icon.filled {
  color: white;
}

/* 좋아요 애니메이션 */
.like-button.animating .heart-icon {
  animation: heartBeat 0.6s ease;
}

@keyframes heartBeat {
  0%, 100% {
    transform: scale(1);
  }
  15% {
    transform: scale(1.3);
  }
  30% {
    transform: scale(1);
  }
  45% {
    transform: scale(1.15);
  }
  60% {
    transform: scale(1);
  }
}

/* Like Count */
.like-count {
  font-weight: 600;
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
  .like-button {
    min-width: 80px;
  }

  .heart-icon {
    width: 1rem;
    height: 1rem;
  }

  .like-count {
    font-size: 0.8125rem;
  }
}
</style>
