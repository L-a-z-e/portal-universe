<script setup lang="ts">
import { computed, ref } from 'vue';
import { Card, Tag, Avatar } from '@portal/design-system-vue';
import type { PostSummaryResponse } from '../dto/post';
import { DEFAULT_THUMBNAILS } from '../config/assets';

interface Props {
  post: PostSummaryResponse;
}
const props = defineProps<Props>();
const emit = defineEmits<{
  click: [postId: string];
}>();

// 썸네일 이미지 에러 핸들링
const imgError = ref(false);
const thumbnailSrc = computed(() => {
  // 에러 발생 시 기본 이미지
  if (imgError.value) {
    return DEFAULT_THUMBNAILS.write;
  }

  // post에 thumbnailUrl이 있으면 사용
  if (props.post.thumbnailUrl) {
    return props.post.thumbnailUrl;
  }

  // 카테고리별 기본 이미지
  const category = props.post.category?.toLowerCase();
  switch (category) {
    case 'travel':
      return DEFAULT_THUMBNAILS.travel;
    case 'tech':
    case 'technology':
      return DEFAULT_THUMBNAILS.tech;
    default:
      return DEFAULT_THUMBNAILS.write;
  }
});

const onImgError = () => {
  imgError.value = true;
};

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

// 요약 텍스트
const summary = computed(() => {
  if (props.post.summary && props.post.summary.trim())
    return props.post.summary.length > 150
        ? props.post.summary.slice(0, 150) + '...'
        : props.post.summary;
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
      class="velog-card group cursor-pointer"
      padding="none"
  >
    <article class="card-container">
      <!-- 썸네일 영역 -->
      <div class="thumbnail-wrapper">
        <img
            :src="thumbnailSrc"
            :alt="post.title"
            class="thumbnail-image"
            @error="onImgError"
        />
      </div>

      <!-- 콘텐츠 영역 -->
      <div class="content-wrapper">
        <!-- 제목 -->
        <h2 class="post-title group-hover:text-brand-primary transition-colors">
          {{ post.title }}
        </h2>

        <!-- 요약 -->
        <p v-if="summary" class="post-summary">
          {{ summary }}
        </p>

        <!-- 하단 메타 정보 -->
        <div class="meta-section">
          <!-- 태그 -->
          <div v-if="post.tags?.length" class="tags-wrapper">
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

          <!-- 작성자 및 통계 -->
          <div class="author-stats-wrapper">
            <div class="author-info">
              <Avatar
                  :name="post.authorName || post.authorId"
                  size="xs"
              />
              <span class="author-name">{{ post.authorName || post.authorId }}</span>
              <span class="separator">·</span>
              <span class="relative-time">{{ relativeTime }}</span>
            </div>

            <div class="stats-wrapper">
              <span class="stat-item">
                <svg class="stat-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                {{ post.viewCount ?? 0 }}
              </span>
              <span class="stat-item">
                <svg class="stat-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                {{ post.likeCount ?? 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </article>
  </Card>
</template>

<style scoped>
/* 카드 컨테이너 - Velog 스타일 */
.card-container {
  display: flex;
  flex-direction: column;
  min-height: 380px;
  height: 100%;
}

/* 썸네일 영역 */
.thumbnail-wrapper {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background-color: var(--color-bg-muted);
  flex-shrink: 0;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.velog-card:hover .thumbnail-image {
  transform: scale(1.05);
}

/* 콘텐츠 영역 */
.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 1rem;
  gap: 0.75rem;
}

/* 제목 */
.post-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-heading);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

/* 요약 */
.post-summary {
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--color-text-body);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  flex: 1;
}

/* 메타 섹션 */
.meta-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: auto;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-muted);
}

/* 태그 영역 */
.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

/* 작성자 및 통계 */
.author-stats-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-meta);
  min-width: 0;
  flex: 1;
}

.author-name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.separator {
  color: var(--color-text-meta);
}

.relative-time {
  white-space: nowrap;
}

/* 통계 영역 */
.stats-wrapper {
  display: flex;
  gap: 0.75rem;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--color-text-meta);
}

.stat-icon {
  width: 1rem;
  height: 1rem;
}

/* 반응형 - 태블릿 이상 */
@media (min-width: 768px) {
  .post-title {
    font-size: 1.25rem;
  }

  .post-summary {
    font-size: 0.9375rem;
  }
}
</style>