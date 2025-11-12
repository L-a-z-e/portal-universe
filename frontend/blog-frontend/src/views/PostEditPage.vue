<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getPostById, updatePost } from '../api/posts';
import type { PostUpdateRequest } from '../dto/PostUpdateRequest';
import { Button, Card, Input, Textarea } from '@portal/design-system';

const props = defineProps<{
  postId: string;
}>();

const router = useRouter();

const title = ref('');
const content = ref('');

const isSubmitting = ref(false);
const error = ref<string | null>(null);
const isLoading = ref(true);
const titleError = ref('');
const contentError = ref('');

onMounted(async () => {
  try {
    const post = await getPostById(props.postId);
    title.value = post.title;
    content.value = post.content;
  } catch (err) {
    console.error('Failed to fetch post for editing:', err);
    error.value = 'Failed to load post data. Please try again.';
  } finally {
    isLoading.value = false;
  }
});

function validate(): boolean {
  let isValid = true;

  titleError.value = '';
  contentError.value = '';
  error.value = null;

  if (!title.value.trim()) {
    titleError.value = '제목을 입력해주세요.';
    isValid = false;
  }

  if (!content.value.trim()) {
    contentError.value = '내용을 입력해주세요.';
    isValid = false;
  }

  return isValid;
}

async function handleSubmit() {
  if (isSubmitting.value) return;

  if (!validate()) return;

  isSubmitting.value = true;
  error.value = null;

  try {
    const payload: PostUpdateRequest = {
      title: title.value.trim(),
      content: content.value.trim(),
    };

    const updatedPost = await updatePost(props.postId, payload);
    alert('게시글이 수정되었습니다!');
    await router.push(`/${updatedPost.id}`);

  } catch (err) {
    console.error('Failed to update post:', err);
    error.value = '게시글 수정에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isSubmitting.value = false;
  }
}

function handleCancel() {
  const confirmed = confirm('수정을 취소하시겠습니까?');
  if (confirmed) {
    router.push(`/${props.postId}`);
  }
}
</script>

<template>
  <div class="max-w-4xl mx-auto p-6">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-4xl font-bold text-gray-900 dark:text-gray-100 mb-2">✏️ 게시글 수정</h1>
      <p class="text-gray-600 dark:text-gray-400">게시글을 수정하세요</p>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="text-center py-20">
      <div class="inline-block w-12 h-12 border-4 border-brand-600 border-t-transparent rounded-full animate-spin"></div>
      <p class="mt-4 text-gray-600 dark:text-gray-400">게시글을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <Card v-else-if="error && !title && !content" variant="outlined" class="bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800">
      <div class="text-center py-8">
        <p class="text-xl text-red-600 dark:text-red-400 mb-4">❌ {{ error }}</p>
        <Button variant="secondary" @click="router.push('/')">
          목록으로 돌아가기
        </Button>
      </div>
    </Card>

    <!-- Form Card -->
    <Card v-else padding="lg">
      <form @submit.prevent="handleSubmit" class="space-y-6">
        <!-- Title Input -->
        <Input
            v-model="title"
            label="제목"
            placeholder="게시글 제목을 입력하세요"
            required
            :error="!!titleError"
            :error-message="titleError"
            :disabled="isSubmitting"
        />

        <!-- Content Textarea -->
        <Textarea
            v-model="content"
            label="내용"
            placeholder="게시글 내용을 입력하세요"
            required
            :rows="15"
            :error="!!contentError"
            :error-message="contentError"
            :disabled="isSubmitting"
        />

        <!-- Error Message -->
        <div v-if="error" class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
          <p class="text-red-600 dark:text-red-400">{{ error }}</p>
        </div>

        <!-- Actions -->
        <div class="flex items-center justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
          <Button
              type="button"
              variant="outline"
              @click="handleCancel"
              :disabled="isSubmitting"
          >
            취소
          </Button>
          <Button
              type="submit"
              variant="primary"
              :disabled="isSubmitting"
          >
            {{ isSubmitting ? '저장 중...' : '💾 수정 완료' }}
          </Button>
        </div>
      </form>
    </Card>
  </div>
</template>