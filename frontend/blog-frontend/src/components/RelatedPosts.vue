<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Spinner } from '@portal/design-vue';
import { getRelatedPosts } from '@/api/posts';
import PostCard from './PostCard.vue';
import type { PostSummaryResponse } from '@/types';

interface Props {
  postId: string;
  tags?: string[];
  limit?: number;
}
const props = withDefaults(defineProps<Props>(), {
  limit: 3
});

const router = useRouter();
const relatedPosts = ref<PostSummaryResponse[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

// 관련 게시글 로드
async function loadRelatedPosts() {
  loading.value = true;
  error.value = null;
  try {
    relatedPosts.value = await getRelatedPosts(props.postId, props.limit);
  } catch (err) {
    console.error('Failed to load related posts:', err);
    error.value = '관련 게시글을 불러올 수 없습니다.';
  } finally {
    loading.value = false;
  }
}

// 포스트 클릭 핸들러
function handlePostClick(postId: string) {
  router.push({ name: 'PostDetail', params: { postId } });
}

onMounted(() => {
  loadRelatedPosts();
});
</script>

<template>
  <section v-if="!loading && !error && relatedPosts.length > 0" class="related-posts">
    <!-- Section Header -->
    <div class="section-header">
      <h2 class="section-title">📚 관련 게시글</h2>
      <p class="section-description">
        비슷한 주제의 다른 글들을 확인해보세요
      </p>
    </div>

    <!-- Posts Grid -->
    <div class="posts-grid">
      <PostCard
        v-for="post in relatedPosts"
        :key="post.id"
        :post="post"
        @click="handlePostClick"
      />
    </div>
  </section>

  <!-- Loading State -->
  <section v-else-if="loading" class="related-posts loading">
    <div class="loading-container">
      <Spinner size="md" class="mx-auto mb-4" />
      <p>관련 게시글을 불러오는 중...</p>
    </div>
  </section>

  <!-- Error State -->
  <section v-else-if="error" class="related-posts error">
    <div class="error-container">
      <p>{{ error }}</p>
    </div>
  </section>
</template>

<style scoped>
.related-posts {
  width: 100%;
  margin-top: 3rem;
  padding-top: 3rem;
  border-top: 2px solid var(--semantic-border-default);
}

/* Section Header */
.section-header {
  margin-bottom: 2rem;
  text-align: center;
}

.section-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--semantic-text-heading);
  margin: 0 0 0.5rem 0;
}

.section-description {
  font-size: 0.9375rem;
  color: var(--semantic-text-meta);
  margin: 0;
}

/* Posts Grid */
.posts-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 640px) {
  .posts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 768px) {
  .posts-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Loading State */
.related-posts.loading {
  border-top: none;
}

.loading-container {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--semantic-text-meta);
}

/* Error State */
.related-posts.error {
  border-top: none;
}

.error-container {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--semantic-status-error);
}

/* 반응형 - 타블릿 */
@media (min-width: 768px) {
  .section-title {
    font-size: 2rem;
  }

  .section-description {
    font-size: 1rem;
  }
}

/* 반응형 - 모바일 */
@media (max-width: 640px) {
  .related-posts {
    margin-top: 2rem;
    padding-top: 2rem;
  }

  .section-header {
    margin-bottom: 1.5rem;
  }

  .section-title {
    font-size: 1.5rem;
  }

  .section-description {
    font-size: 0.875rem;
  }

  .posts-grid {
    gap: 1rem;
  }
}
</style>
