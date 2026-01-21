<script setup lang="ts">
import { computed } from 'vue';
import { Avatar, Card } from '@portal/design-system-vue';
import type { UserProfileResponse } from '@/dto/user';

interface Props {
  user: UserProfileResponse;
}
const props = defineProps<Props>();

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
</script>

<template>
  <Card class="user-profile-card" padding="lg">
    <div class="profile-container">
      <!-- 프로필 이미지 -->
      <div class="avatar-section">
        <Avatar
          :src="user.profileImageUrl"
          :name="user.name || user.username || user.email"
          size="2xl"
          class="profile-avatar"
        />
      </div>

      <!-- 프로필 정보 -->
      <div class="info-section">
        <!-- 이름 -->
        <h2 class="user-name">{{ user.name }}</h2>

        <!-- Username -->
        <p v-if="user.username" class="username">@{{ user.username }}</p>

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

.user-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-heading);
  margin: 0;
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

  .website-link {
    margin: 0;
  }

  .join-date {
    justify-content: flex-start;
  }
}
</style>
