<script setup lang="ts">
import { useAuthStore } from "portal_shell/authStore";
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getAllPosts } from "../api/posts.ts";
import type { PostResponse } from "../dto/PostResponse.ts";
import { Button, Card, Badge } from '@portal/design-system';

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
  <div class="max-w-5xl mx-auto px-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">📝 Blog Posts</h1>
        <p class="text-gray-600 dark:text-gray-400">모든 게시글을 확인하세요</p>
      </div>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          @click="router.push('/write')"
      >
        ✍️ 새 글 작성
      </Button>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="text-center py-20">
      <div class="inline-block w-12 h-12 border-4 border-brand-600 border-t-transparent rounded-full animate-spin"></div>
      <p class="mt-4 text-gray-600 dark:text-gray-400">게시글을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <Card v-else-if="error" variant="outlined" class="bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
      <div class="text-center py-8">
        <p class="text-xl text-red-600 dark:text-red-400 mb-4">❌ {{ error }}</p>
        <Button variant="secondary" @click="$router.go(0)">
          다시 시도
        </Button>
      </div>
    </Card>

    <!-- Empty State -->
    <Card v-else-if="posts.length === 0" class="text-center py-16">
      <div class="text-6xl mb-4">📭</div>
      <h3 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">아직 게시글이 없습니다</h3>
      <p class="text-gray-600 dark:text-gray-400 mb-6">첫 게시글을 작성해보세요!</p>
      <Button
          v-if="authStore.isAuthenticated"
          variant="primary"
          @click="router.push('/write')"
      >
        첫 글 작성하기
      </Button>
    </Card>

    <!-- Post Grid -->
    <div v-else class="grid gap-6">
      <Card
          v-for="post in posts"
          :key="post.id"
          hoverable
          @click="goToPost(post.id)"
          class="cursor-pointer"
          padding="lg"
      >
        <div class="flex flex-col gap-4">
          <!-- Title & Badge -->
          <div class="flex items-start justify-between gap-4">
            <h3 class="text-xl font-bold text-gray-900 dark:text-gray-100 hover:text-brand-600 dark:hover:text-brand-400 transition-colors flex-1">
              {{ post.title }}
            </h3>
            <Badge variant="primary" size="sm" class="flex-shrink-0">
              New
            </Badge>
          </div>

          <!-- Content Preview -->
          <p class="text-gray-600 dark:text-gray-400 line-clamp-2 leading-relaxed">
            {{ post.content }}
          </p>

          <!-- Meta Info -->
          <div class="flex items-center gap-6 text-sm text-gray-500 dark:text-gray-500 pt-3 border-t border-gray-100 dark:border-gray-700">
            <span class="flex items-center gap-2">
              <span>👤</span>
              <span class="font-medium">{{ post.authorId }}</span>
            </span>
            <span class="flex items-center gap-2">
              <span>📅</span>
              {{ new Date(post.createdAt).toLocaleDateString('ko-KR', {
              year: 'numeric',
              month: 'long',
              day: 'numeric'
            }) }}
            </span>
            <span class="ml-auto text-brand-600 dark:text-brand-400 font-medium">
              자세히 보기 →
            </span>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>