<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { createPost } from '../api/posts';
import type { PostCreateRequest } from "../dto/PostCreateRequest.ts";
import { Button, Card, Input, Textarea } from '@portal/design-system';

const router = useRouter();

const title = ref('');
const content = ref('');
const productId = ref('1');

const isSubmitting = ref(false);
const error = ref<string | null>(null);
const titleError = ref('');
const contentError = ref('');

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
    const payload: PostCreateRequest = {
      title: title.value.trim(),
      content: content.value.trim(),
      productId: productId.value,
    };

    const newPost = await createPost(payload);
    alert('게시글이 작성되었습니다!');
    await router.push(`/${newPost.id}`);

  } catch (err) {
    console.error('Failed to create post:', err);
    error.value = '게시글 작성에 실패했습니다. 다시 시도해주세요.';
  } finally {
    isSubmitting.value = false;
  }
}

function handleCancel() {
  if (title.value || content.value) {
    const confirmed = confirm('작성 중인 내용이 있습니다. 취소하시겠습니까?');
    if (!confirmed) return;
  }
  router.push('/');
}
</script>

<template>
  <div class="max-w-4xl mx-auto p-6">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-4xl font-bold text-gray-900 dark:text-gray-100 mb-2">✍️ 새 글 작성</h1>
      <p class="text-gray-600 dark:text-gray-400">멋진 게시글을 작성해보세요</p>
    </div>

    <!-- Form Card -->
    <Card padding="lg">
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
            {{ isSubmitting ? '저장 중...' : '📝 게시글 작성' }}
          </Button>
        </div>
      </form>
    </Card>
  </div>
</template>