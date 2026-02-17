<script setup lang="ts">
import { computed, ref } from 'vue';
import { Avatar } from '@portal/design-vue';
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
  <div class="flex flex-col items-center mb-12">
    <!-- 아바타 -->
    <div class="relative mb-6">
      <Avatar
        :src="user.profileImageUrl ?? undefined"
        :name="user.nickname || user.username || user.email"
        size="2xl"
        class="!w-20 !h-20 border-2 border-bg-page shadow-2xl"
      />
    </div>

    <!-- 이름 -->
    <h1 class="text-2xl font-bold text-text-heading mb-2">{{ user.nickname }}</h1>

    <!-- Username -->
    <p v-if="user.username" class="text-text-meta mb-2">@{{ user.username }}</p>

    <!-- Bio -->
    <p v-if="user.bio" class="text-text-meta text-center max-w-md mb-4 leading-relaxed">
      {{ user.bio }}
    </p>

    <!-- 통계 -->
    <div class="flex items-center gap-6 text-sm text-text-meta mb-8">
      <button class="hover:text-text-heading transition-colors" @click="openFollowerModal">
        <strong class="text-text-heading">{{ followerCount }}</strong> 팔로워
      </button>
      <span class="w-1 h-1 rounded-full bg-border-default"></span>
      <button class="hover:text-text-heading transition-colors" @click="openFollowingModal">
        <strong class="text-text-heading">{{ followingCount }}</strong> 팔로잉
      </button>
    </div>

    <!-- 액션 버튼 -->
    <div class="flex items-center gap-3">
      <FollowButton
        v-if="!isCurrentUser && user.username"
        :username="user.username"
        :target-uuid="user.uuid"
        size="md"
        class="!rounded-full !px-6"
        @follow-changed="handleFollowChanged"
      />

      <!-- Website 링크 -->
      <a
        v-if="websiteUrl"
        :href="websiteUrl"
        target="_blank"
        rel="noopener noreferrer"
        class="w-9 h-9 flex items-center justify-center rounded-full border border-border-default text-text-meta hover:text-text-heading hover:bg-bg-hover transition-colors"
        :title="user.website ?? undefined"
      >
        <svg class="w-[18px] h-[18px]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"
          />
        </svg>
      </a>
    </div>

    <!-- 가입일 -->
    <div class="flex items-center gap-2 text-xs text-text-meta mt-6">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

  <!-- 팔로워/팔로잉 모달 -->
  <FollowerModal
    v-if="user.username"
    :username="user.username"
    :is-open="modalOpen"
    :type="modalType"
    @close="modalOpen = false"
  />
</template>
