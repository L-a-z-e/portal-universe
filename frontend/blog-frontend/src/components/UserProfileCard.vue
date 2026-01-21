<script setup lang="ts">
import { computed, ref } from 'vue';
import { Avatar, Card } from '@portal/design-system-vue';
import type { UserProfileResponse } from '@/dto/user';
import FollowButton from './FollowButton.vue';
import FollowerModal from './FollowerModal.vue';

interface Props {
  user: UserProfileResponse;
  isCurrentUser?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  isCurrentUser: false
});

const emit = defineEmits<{
  followChanged: [followerCount: number, followingCount: number];
}>();

// State
const followerCount = ref(props.user.followerCount);
const followingCount = ref(props.user.followingCount);
const modalOpen = ref(false);
const modalType = ref<'followers' | 'following'>('followers');

// 가입일 포맷팅
const formattedDate = computed(() => {
  return new Date(props.user.createdAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
});

// Website URL 정리
const websiteUrl = computed(() => {
  if (!props.user.website) return null;
  return props.user.website.startsWith('http')
    ? props.user.website
    : `https://${props.user.website}`;
});

// 팔로우 변경 처리
function handleFollowChanged(_following: boolean, newFollowerCount: number, newFollowingCount: number) {
  followerCount.value = newFollowerCount;
  followingCount.value = newFollowingCount;
  emit('followChanged', newFollowerCount, newFollowingCount);
}

// 팔로워/팔로잉 모달 열기
function openFollowerModal() {
  modalType.value = 'followers';
  modalOpen.value = true;
}

function openFollowingModal() {
  modalType.value = 'following';
  modalOpen.value = true;
}
</script>

<template>
  <Card class="user-profile-card" padding="lg">
    <div class="profile-container">
      <!-- 프로필 이미지 -->
      <div class="avatar-section">
        <Avatar
          :src="user.profileImageUrl ?? undefined"
          :name="user.nickname || user.username || user.email"
          size="2xl"
          class="profile-avatar"
        />
      </div>

      <!-- 프로필 정보 -->
      <div class="info-section">
        <!-- 이름 + 팔로우 버튼 -->
        <div class="name-row">
          <h2 class="user-name">{{ user.nickname }}</h2>
          <FollowButton
            v-if="!isCurrentUser && user.username"
            :username="user.username"
            :target-uuid="user.uuid"
            size="sm"
            @follow-changed="handleFollowChanged"
          />
        </div>

        <!-- Username -->
        <p v-if="user.username" class="username">@{{ user.username }}</p>

        <!-- 팔로워/팔로잉 수 -->
        <div class="follow-stats">
          <button class="stat-button" @click="openFollowerModal">
            <span class="stat-count">{{ followerCount }}</span>
            <span class="stat-label">팔로워</span>
          </button>
          <button class="stat-button" @click="openFollowingModal">
            <span class="stat-count">{{ followingCount }}</span>
            <span class="stat-label">팔로잉</span>
          </button>
        </div>

        <!-- Bio -->
        <p v-if="user.bio" class="bio">{{ user.bio }}</p>

        <!-- Website -->
        <a
          v-if="websiteUrl"
          :href="websiteUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="website-link"
        >
          <svg class="link-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"
            />
          </svg>
          {{ user.website }}
        </a>

        <!-- 가입일 -->
        <div class="join-date">
          <svg class="calendar-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg>
          <span>{{ formattedDate }} 가입</span>
        </div>
      </div>
    </div>

    <!-- 팔로워/팔로잉 모달 -->
    <FollowerModal
      v-if="user.username"
      :username="user.username"
      :is-open="modalOpen"
      :type="modalType"
      @close="modalOpen = false"
    />
  </Card>
</template>

<style scoped>
.profile-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 1.5rem;
}

.avatar-section {
  flex-shrink: 0;
}

.profile-avatar {
  border: 3px solid var(--color-border-default);
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
}

.name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.user-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-heading);
  margin: 0;
}

/* 팔로우 통계 */
.follow-stats {
  display: flex;
  justify-content: center;
  gap: 1.5rem;
  margin: 0.5rem 0;
}

.stat-button {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.5rem;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 0.25rem;
  transition: background 0.2s;
}

.stat-button:hover {
  background: var(--color-surface-alt);
}

.stat-count {
  font-weight: 700;
  color: var(--color-text-heading);
}

.stat-label {
  color: var(--color-text-meta);
  font-size: 0.875rem;
}

.username {
  font-size: 1rem;
  color: var(--color-text-meta);
  margin: 0;
}

.bio {
  font-size: 0.9375rem;
  line-height: 1.6;
  color: var(--color-text-body);
  margin: 0;
  white-space: pre-wrap;
}

.website-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-brand-primary);
  text-decoration: none;
  transition: color 0.2s;
  width: fit-content;
  margin: 0 auto;
}

.website-link:hover {
  color: var(--color-brand-primary-hover);
  text-decoration: underline;
}

.link-icon {
  width: 1.125rem;
  height: 1.125rem;
  flex-shrink: 0;
}

.join-date {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--color-text-meta);
  margin-top: 0.5rem;
}

.calendar-icon {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

/* 반응형 - 태블릿 이상 */
@media (min-width: 768px) {
  .profile-container {
    flex-direction: row;
    text-align: left;
    align-items: flex-start;
  }

  .info-section {
    align-items: flex-start;
  }

  .name-row {
    justify-content: flex-start;
  }

  .follow-stats {
    justify-content: flex-start;
  }

  .website-link {
    margin: 0;
  }

  .join-date {
    justify-content: flex-start;
  }
}
</style>
