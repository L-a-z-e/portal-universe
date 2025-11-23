<script setup lang="ts">
import { computed } from 'vue';
import { Card, Tag, Avatar } from '@portal/design-system';
import type { PostSummaryResponse } from '../dto/post';

interface Props {
  post: PostSummaryResponse;
}
const props = defineProps<Props>();
const emit = defineEmits<{
  click: [postId: string];
}>();

// 날짜 포맷팅
const formattedDate = computed(() => {
  return new Date(props.post.publishedAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
});

// 상대 시간 (예: "3일 전")
const relativeTime = computed(() => {
  const now = new Date();
  const published = new Date(props.post.publishedAt);
  const diff = now.getTime() - published.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  return formattedDate.value;
});

// 요약 텍스트 (summary 필드 활용, 만약 없다면 content의 앞부분)
const summary = computed(() => {
  if (props.post.summary && props.post.summary.trim())
    return props.post.summary.length > 150
        ? props.post.summary.slice(0, 150) + '...'
        : props.post.summary;
  // fallback
  const clean = (props.post as any).content?.replace(/<[^>]*>/g, '') || '';
  return clean.length > 150 ? clean.substring(0, 150) + '...' : clean;
});

const handleClick = () => {
  emit('click', props.post.id);
};
</script>

<template>
  <Card
      hoverable
      @click="handleClick"
      class="cursor-pointer card-hover"
      padding="lg"
  >
    <article class="flex flex-col gap-4">
      <!-- 작성자 정보 -->
      <div class="flex items-center gap-3">
        <Avatar
            :name="post.authorName || post.authorId"
            size="sm"
        />
        <div class="flex-1 min-w-0">
          <p class="font-semibold text-text-heading truncate">
            {{ post.authorName || post.authorId }}
          </p>
          <p class="text-sm text-text-meta">
            {{ relativeTime }}
          </p>
        </div>
      </div>

      <!-- 제목 -->
      <h2 class="post-title line-clamp-2">
        {{ post.title }}
      </h2>

      <!-- 요약 -->
      <p class="text-text-body post-summary line-clamp-3">
        {{ summary }}
      </p>

      <!-- 태그 -->
      <div v-if="post.tags?.length" class="flex flex-wrap gap-2">
        <Tag
            v-for="tag in post.tags.slice(0, 3)"
            :key="tag"
            variant="default"
            size="sm"
        >
          {{ tag }}
        </Tag>
        <Tag
            v-if="post.tags.length > 3"
            variant="default"
            size="sm"
        >
          +{{ post.tags.length - 3 }}
        </Tag>
      </div>

      <!-- 구분선 (stats-divider) -->
      <div class="stats-divider"></div>

      <!-- 통계 정보 -->
      <div class="flex items-center gap-4 text-sm text-text-meta">
        <span class="flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          {{ post.viewCount ?? 0 }}
        </span>
        <span class="flex items-center gap-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          {{ post.likeCount ?? 0 }}
        </span>
        <span class="ml-auto text-brand-primary font-medium flex items-center gap-1">
          자세히 보기
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
          </svg>
        </span>
      </div>
    </article>
  </Card>
</template>

<style scoped>
.post-title {
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--color-text-heading);
  transition: color 0.2s;
}
.card-hover:hover .post-title {
  color: var(--color-brand-primary);
}
.post-summary {
  line-height: 1.7;
}
.stats-divider {
  border-top: 1px solid var(--color-border-muted);
  margin-top: 0.75rem;
  margin-bottom: 0.5rem;
}
/* line-clamp 유틸 없으면 직접 구현 */
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>