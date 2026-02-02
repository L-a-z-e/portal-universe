<script setup lang="ts">
import { computed, ref } from 'vue';
import { Card } from '@portal/design-system-vue';
import type { SeriesListResponse } from '../dto/series';
import { DEFAULT_THUMBNAILS } from '../config/assets';

interface Props {
  series: SeriesListResponse;
}
const props = defineProps<Props>();
const emit = defineEmits<{
  click: [seriesId: string];
}>();

// 썸네일 이미지 에러 핸들링
const imgError = ref(false);
const thumbnailSrc = computed(() => {
  if (imgError.value) {
    return DEFAULT_THUMBNAILS.write;
  }
  return props.series.thumbnailUrl || DEFAULT_THUMBNAILS.write;
});

const onImgError = () => {
  imgError.value = true;
};

// 날짜 포맷팅
const formattedDate = computed(() => {
  return new Date(props.series.updatedAt).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
});

const handleClick = () => {
  emit('click', props.series.id);
};
</script>

<template>
  <Card
      hoverable
      @click="handleClick"
      class="series-card group cursor-pointer"
      padding="none"
  >
    <article class="card-container">
      <!-- 썸네일 영역 -->
      <div class="thumbnail-wrapper">
        <img
            :src="thumbnailSrc"
            :alt="series.name"
            class="thumbnail-image"
            @error="onImgError"
        />
        <!-- 시리즈 배지 -->
        <div class="series-badge">
          <svg class="badge-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <span>시리즈</span>
        </div>
      </div>

      <!-- 콘텐츠 영역 -->
      <div class="content-wrapper">
        <!-- 시리즈 이름 -->
        <h2 class="series-title group-hover:text-brand-primary transition-colors">
          {{ series.name }}
        </h2>

        <!-- 설명 -->
        <p v-if="series.description" class="series-description">
          {{ series.description }}
        </p>

        <!-- 하단 메타 정보 -->
        <div class="meta-section">
          <div class="meta-info">
            <span class="author-name">{{ series.authorName }}</span>
            <span class="separator">·</span>
            <span class="post-count">{{ series.postCount }}개의 글</span>
          </div>
          <div class="update-time">{{ formattedDate }}</div>
        </div>
      </div>
    </article>
  </Card>
</template>

<style scoped>
/* 카드 컨테이너 */
.card-container {
  display: flex;
  flex-direction: column;
  min-height: 340px;
  height: 100%;
}

/* 썸네일 영역 */
.thumbnail-wrapper {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background-color: var(--semantic-bg-muted);
  flex-shrink: 0;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.series-card:hover .thumbnail-image {
  transform: scale(1.05);
}

/* 시리즈 배지 */
.series-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.625rem;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 0.375rem;
  backdrop-filter: blur(4px);
}

.badge-icon {
  width: 0.875rem;
  height: 0.875rem;
}

/* 콘텐츠 영역 */
.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 1rem;
  gap: 0.75rem;
}

/* 시리즈 제목 */
.series-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

/* 설명 */
.series-description {
  font-size: 0.875rem;
  line-height: 1.6;
  color: var(--semantic-text-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  flex: 1;
}

/* 메타 섹션 */
.meta-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 0.75rem;
  border-top: 1px solid var(--semantic-border-muted);
  font-size: 0.75rem;
  color: var(--semantic-text-meta);
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.author-name {
  font-weight: 500;
}

.separator {
  color: var(--semantic-text-meta);
}

.post-count {
  font-weight: 600;
  color: var(--semantic-brand-primary);
}

.update-time {
  white-space: nowrap;
}

/* 반응형 - 태블릿 이상 */
@media (min-width: 768px) {
  .series-title {
    font-size: 1.25rem;
  }

  .series-description {
    font-size: 0.9375rem;
  }
}
</style>
