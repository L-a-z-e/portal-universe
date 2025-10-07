<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { createPost } from '../api/posts'; // 👈 API 함수 import
import type { PostCreateRequest } from "../dto/PostCreateRequest.ts";

const router = useRouter();

const title = ref('');
const content = ref('');
// 임시로 하드코딩
const productId = ref('1');

const isSubmitting = ref(false);
const error = ref<string | null>(null);

async function handleSubmit() {
  // 이미 제출 중이면 중복 실행 방지
  if (isSubmitting.value) return;

  // 간단한 유효성 검사
  if (!title.value || !content.value) {
    error.value = 'Title and content are required.';
    return;
  }

  isSubmitting.value = true;
  error.value = null;

  try {
    const payload: PostCreateRequest = {
      title: title.value,
      content: content.value,
      productId: productId.value,
    };

    const newPost = await createPost(payload);

    alert('Post created successfully!');
    await router.push(`/${newPost.id}`);

  } catch (err) {
    console.error('Failed to create post:', err);
    error.value = 'Failed to create post. Please try again.';
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div>
    <h2>Write a New Post</h2>

    <!-- @submit.prevent는 폼 제출 시 페이지가 새로고침되는 기본 동작을 막는다. -->
    <form @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="title">Title</label>
        <input id="title" v-model="title" type="text" />
      </div>
      <div class="form-group">
        <label for="content">Content</label>
        <textarea id="content" v-model="content" rows="10"></textarea>
      </div>

      <!-- 에러 메시지 표시 -->
      <p v-if="error" class="error-message">{{ error }}</p>

      <div class="form-actions">
        <button type="button" @click="router.push('/')" :disabled="isSubmitting">Cancel</button>
        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Saving...' : 'Save Post' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-group {
  margin-bottom: 1rem;
}
label {
  display: block;
  margin-bottom: 0.5rem;
}
input, textarea {
  width: 100%;
  padding: 0.5rem;
  font-size: 1rem;
  box-sizing: border-box; /* padding이 width에 포함되도록 설정 */
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}
.error-message {
  color: red;
}
</style>