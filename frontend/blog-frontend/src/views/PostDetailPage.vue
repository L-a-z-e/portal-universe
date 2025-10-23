<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchPostById, deletePost } from '../api/posts';
import type { PostResponse } from '../dto/PostResponse';
import { Button, Card, Badge } from '@portal/design-system';

const props = defineProps<{
  postId: string;
}>();

const router = useRouter();
const post = ref<PostResponse | null>(null);
const error = ref<string | null>(null);
const isLoading = ref(true);
const isDeleting = ref(false);

onMounted(async () => {
  try {
    post.value = await fetchPostById(props.postId);
  } catch (err) {
    console.error('Failed to fetch post:', err);
    error.value = 'Failed to load the post. It may not exist or an error occurred.';
  } finally {
    isLoading.value = false;
  }
});

async function handleDelete() {
  if (!post.value || isDeleting.value) return;

  const confirmed = confirm('정말로 이 게시글을 삭제하시겠습니까?');
  if (!confirmed) return;

  try {
    isDeleting.value = true;
    await deletePost(post.value.id);
    alert('게시글이 삭제되었습니다.');
    router.push('/');
  } catch (err) {
    console.error('Failed to delete post:', err);
    error.value = 'Failed to delete the post. Please try again.';
  } finally {
    isDeleting.value = false;
  }
}
</script>

<template>
  <div class="max-w-4xl mx-auto p-6">
    <!-- Loading -->
    <div v-if="isLoading" class="text-center py-20">
      <div class="inline-block w-12 h-12 border-4 border-brand-600 border-t-transparent rounded-full animate-spin"></div>
      <p class="mt-4 text-gray-600 dark:text-gray-400">게시글을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <Card v-else-if="error" variant="outlined" class="bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
      <div class="text-center py-8">
        <p class="text-xl text-red-600 dark:text-red-400 mb-4">❌ {{ error }}</p>
        <Button variant="secondary" @click="router.push('/')">
          목록으로 돌아가기
        </Button>
      </div>
    </Card>

    <!-- Post Content -->
    <div v-else-if="post">
      <!-- Back Button -->
      <Button variant="outline" size="sm" @click="router.push('/')" class="mb-6">
        ← 목록으로
      </Button>

      <!-- Post Card -->
      <Card padding="lg">
        <!-- Header -->
        <div class="border-b border-gray-200 dark:border-gray-700 pb-6 mb-6">
          <div class="flex items-start justify-between mb-4">
            <h1 class="text-3xl font-bold text-gray-900 dark:text-gray-100 flex-1">
              {{ post.title }}
            </h1>
            <Badge variant="success" size="sm">
              Published
            </Badge>
          </div>

          <div class="flex items-center gap-6 text-sm text-gray-500 dark:text-gray-500">
            <span class="flex items-center gap-2">
              <span>👤</span>
              <span class="font-medium text-gray-700 dark:text-gray-300">{{ post.authorId }}</span>
            </span>
            <span class="flex items-center gap-2">
              <span>📅</span>
              {{ new Date(post.createdAt).toLocaleString('ko-KR') }}
            </span>
            <span class="flex items-center gap-2">
              <span>✏️</span>
              {{ new Date(post.updatedAt).toLocaleString('ko-KR') }}
            </span>
          </div>
        </div>

        <!-- Content -->
        <div class="prose max-w-none mb-8">
          <p class="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
            {{ post.content }}
          </p>
        </div>

        <!-- Actions -->
        <div class="flex items-center justify-between pt-6 border-t border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <Button variant="primary" @click="router.push(`/edit/${post.id}`)">
              ✏️ 수정
            </Button>
            <Button
                variant="secondary"
                @click="handleDelete"
                :disabled="isDeleting"
            >
              {{ isDeleting ? '삭제 중...' : '🗑️ 삭제' }}
            </Button>
          </div>

          <Button variant="outline" @click="router.push('/')">
            목록으로
          </Button>
        </div>
      </Card>
    </div>

    <!-- Not Found -->
    <Card v-else class="text-center py-16">
      <div class="text-6xl mb-4">🔍</div>
      <h3 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">게시글을 찾을 수 없습니다</h3>
      <p class="text-gray-600 dark:text-gray-400 mb-6">삭제되었거나 존재하지 않는 게시글입니다.</p>
      <Button variant="primary" @click="router.push('/')">
        목록으로 돌아가기
      </Button>
    </Card>
  </div>
</template>