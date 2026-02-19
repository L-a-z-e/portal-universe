<script setup lang="ts">
import { computed, ref } from 'vue';
import { Avatar } from '@portal/design-vue';
import type { PostSummaryResponse } from '../dto/post';

import { useRelativeTime } from '@/composables/useRelativeTime';

interface Props {
  post: PostSummaryResponse;
}
const props = defineProps<Props>();
const emit = defineEmits<{
  click: [postId: string];
}>();

// 썸네일 이미지 에러 핸들링
const imgError = ref(false);
const hasThumbnail = computed(() => !!props.post.thumbnailUrl && !imgError.value);
const thumbnailSrc = computed(() => {
  if (imgError.value || !props.post.thumbnailUrl) return null;
  return props.post.thumbnailUrl;
});
const onImgError = () => {
  imgError.value = true;
};

// 상대 시간 (composable 사용)
const { relativeTime } = useRelativeTime(
  computed(() => props.post.publishedAt)
);

// 읽기 시간 추정
const readTime = computed(() => {
  const text = props.post.summary || '';
  const words = text.length;
  const minutes = Math.max(1, Math.ceil(words / 500));
  return `${minutes} min read`;
});

const handleClick = () => {
  emit('click', props.post.id);
};
</script>

<template>
  <article
    class="group cursor-pointer"
    @click="handleClick"
  >
    <div class="flex gap-6 py-8">
      <!-- 좌측: 콘텐츠 -->
      <div class="flex-1 min-w-0 flex flex-col gap-2">
        <!-- 카테고리 -->
        <span v-if="post.category" class="text-sm font-medium text-brand-primary">
          {{ post.category }}
        </span>

        <!-- 제목 -->
        <h2 class="text-xl font-bold text-text-heading leading-tight group-hover:text-brand-primary transition-colors">
          {{ post.title }}
        </h2>

        <!-- 요약 -->
        <p v-if="post.summary" class="text-text-body leading-relaxed line-clamp-2 text-base">
          {{ post.summary }}
        </p>

        <!-- 메타 정보 -->
        <div class="flex items-center gap-2 mt-2 text-xs text-text-meta">
          <component
            :is="post.authorUsername ? 'router-link' : 'div'"
            :to="post.authorUsername ? `/@${post.authorUsername}` : undefined"
            @click.stop
            class="flex items-center gap-2"
            :class="{ 'hover:text-brand-primary transition-colors': post.authorUsername }"
          >
            <Avatar
              :src="undefined"
              :name="post.authorNickname || '사용자'"
              size="xs"
            />
            <span class="font-medium text-text-heading text-sm">{{ post.authorNickname || '사용자' }}</span>
          </component>
          
          <span class="ml-1">·</span>
          <span>{{ relativeTime }}</span>
          <span>·</span>
          <span>{{ readTime }}</span>
          <template v-if="(post.likeCount ?? 0) > 0">
            <span>·</span>
            <span class="flex items-center gap-1">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
              {{ post.likeCount }}
            </span>
          </template>
        </div>
      </div>

      <!-- 우측: 썸네일 (있을 때만) -->
      <div v-if="hasThumbnail" class="hidden sm:block flex-shrink-0">
        <img
          :src="thumbnailSrc!"
          :alt="post.title"
          class="w-28 h-20 rounded-lg object-cover bg-bg-muted"
          @error="onImgError"
        />
      </div>
    </div>

    <!-- 구분선 -->
    <div class="border-b border-border-default"></div>
  </article>
</template>
