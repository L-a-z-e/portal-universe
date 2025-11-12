<script setup lang="ts">
import { useAuthStore } from "portal_shell/authStore";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getAllPosts } from "../api/posts.ts";
import type { PostResponse } from "../dto/PostResponse.ts";
import { Button, Card } from '@portal/design-system';
import PostCard from '../components/PostCard.vue';

const router = useRouter();
const authStore = useAuthStore();
const posts = ref<PostResponse[]>([]);
const isLoading = ref(true);
const error = ref<string | null>(null);

onMounted(async () => {
  try {
    isLoading.value = true;
    error.value = null;
    posts.value = await getAllPosts();
  } catch (err) {
    console.error('Failed to fetch posts:', err);
    error.value = 'Failed to fetch posts. Please try again later.';
  } finally {
    isLoading.value = false;
  }
});

function goToPost(postId: string) {
  router.push(`/${postId}`);
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 sm:px-6 py-8">
    <!-- Header -->
    <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-10">
      <div>
        <h1 class="text-3xl sm:text-4xl font-bold text-text-heading mb-2">
          📝 Blog
        </h1>
        <p class="text-text-meta">
          {{ posts.length }}개의 게시글
        </p>
      </div>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          size="md"
          @click="router.push('/write')"
      >
        ✍️ 새 글 작성
      </Button>
    </header>

    <!-- Loading -->
    <div v-if="isLoading" class="text-center py-20">
      <div class="inline-block w-12 h-12 border-4 border-brand-primary border-t-transparent rounded-full animate-spin"></div>
      <p class="mt-4 text-text-meta">게시글을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <Card
        v-else-if="error"
        class="bg-status-error-bg border-2 border-status-error/20"
    >
      <div class="text-center py-8">
        <div class="text-5xl mb-4">❌</div>
        <p class="text-xl text-status-error mb-4">{{ error }}</p>
        <Button variant="secondary" @click="$router.go(0)">
          다시 시도
        </Button>
      </div>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="posts.length === 0" class="text-center py-16">
      <div class="text-6xl mb-4">📭</div>
      <h3 class="text-2xl font-bold text-text-heading mb-2">
        아직 게시글이 없습니다
      </h3>
      <p class="text-text-meta mb-6">
        첫 게시글을 작성해보세요!
      </p>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          @click="router.push('/write')"
      >
        첫 글 작성하기
      </Button>
    </Card>

    <!-- Post Grid -->
    <div v-else class="grid gap-6 sm:gap-8">
      <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          @click="goToPost"
      />
    </div>

    <!-- Load More (나중에 무한 스크롤로 전환) -->
    <div v-if="posts.length > 0" class="text-center mt-12">
      <Button variant="secondary" size="lg" disabled>
        더 보기
      </Button>
      <p class="text-sm text-text-meta mt-2">
        무한 스크롤 준비 중...
      </p>
    </div>
  </div>
</template>

<style scoped>
/* 반응형 그리드 (나중에 확장 가능) */
@media (min-width: 1024px) {
  /* 큰 화면에서는 2컬럼 레이아웃도 고려 가능 */
}
</style>