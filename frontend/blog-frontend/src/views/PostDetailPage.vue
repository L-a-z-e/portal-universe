<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getPostById } from "../api/posts";
import { Button, Tag, Avatar, Card } from "@portal/design-system";
import type { PostResponse } from "../dto/PostResponse";

const route = useRoute();
const router = useRouter();
const post = ref<PostResponse | null>(null);
const isLoading = ref(true);
const error = ref<string | null>(null);

onMounted(async () => {
  const postId = route.params.postId as string;
  if (!postId) {
    error.value = "존재하지 않는 게시글입니다";
    isLoading.value = false;
    return;
  }
  try {
    isLoading.value = true;
    error.value = null;
    post.value = await getPostById(postId);
  } catch (err) {
    error.value = "게시글을 가져오지 못했습니다.";
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <!-- Loading & Error -->
    <div v-if="isLoading" class="text-center py-24">
      <div class="w-10 h-10 border-4 border-brand-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
      <p class="text-text-meta">게시글을 불러오는 중...</p>
    </div>
    <Card v-else-if="error" class="bg-status-error-bg border-status-error/30 py-16 text-center">
      <div class="text-2xl text-status-error mb-4">❌</div>
      <div>{{ error }}</div>
      <Button variant="secondary" class="mt-5" @click="router.back()">돌아가기</Button>
    </Card>

    <!-- Post Detail -->
    <article v-else-if="post" class="space-y-8">
      <header class="flex flex-col gap-2 border-b border-border-muted pb-4">
        <h1 class="text-3xl font-bold text-text-heading break-words">{{ post.title }}</h1>
        <!-- Author & Date -->
        <div class="flex items-center gap-3">
          <Avatar :name="post.authorName || post.authorId" size="sm" />
          <div class="flex flex-col">
            <span class="font-semibold text-text-heading truncate">{{ post.authorName || post.authorId }}</span>
            <span class="text-sm text-text-meta">{{ new Date(post.createdAt).toLocaleString() }}</span>
          </div>
          <span class="ml-auto flex items-center gap-3">
            <span class="flex items-center gap-1 text-sm text-text-meta"><span>👁</span>{{ post.viewCount || 0 }}</span>
            <span class="flex items-center gap-1 text-sm text-text-meta"><span>❤️</span>{{ post.likeCount || 0 }}</span>
          </span>
        </div>
        <!-- Tags -->
        <div v-if="post.tags && post.tags.length" class="flex flex-wrap gap-2 mt-2">
          <Tag v-for="tag in post.tags" :key="tag" variant="default" size="sm">{{ tag }}</Tag>
        </div>
      </header>

      <!-- Content (마크다운/HTML 렌더) -->
      <section>
        <!-- TODO: 마크다운 Viewer로 교체 예정 -->
        <div
            class="prose dark:prose-invert max-w-none"
            v-html="post.content"
        />
      </section>

      <footer class="border-t border-border-muted pt-6 text-text-meta text-sm">
        <div v-if="post.category">카테고리: <b>{{ post.category }}</b></div>
        <div v-if="post.publishedAt">최초 발행: {{ new Date(post.publishedAt).toLocaleDateString() }}</div>
        <div>최종 수정: {{ new Date(post.updatedAt).toLocaleDateString() }}</div>
      </footer>

      <!-- 댓글 영역 Placeholder -->
      <div class="mt-14">
        <Card variant="outlined" class="bg-bg-muted border-border-muted text-center py-12">
          <div class="text-3xl mb-2">💬</div>
          <div>댓글 기능 준비중...</div>
        </Card>
      </div>
    </article>
  </div>
</template>