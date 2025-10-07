<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchPostById, updatePost } from '../api/posts';
import type { PostUpdateRequest } from '../dto/PostUpdateRequest';

const props = defineProps<{
  postId: string;
}>();

const router = useRouter();

const title = ref('');
const content = ref('');

const isSubmitting = ref(false);
const error = ref<string | null>(null);
const isLoading = ref(true);

onMounted(async () => {
  try {
    const post = await fetchPostById(props.postId);
    title.value = post.title;
    content.value = post.content;
  } catch (err) {
    console.error('Failed to fetch post for editing:', err);
    error.value = 'Failed to load post data. Please try again.';
  } finally {
    isLoading.value = false;
  }
});

async function handleSubmit() {
  if (isSubmitting.value) return;

  if (!title.value || !content.value) {
    error.value = 'Title and content are required.';
    return;
  }

  isSubmitting.value = true;
  error.value = null;

  try {
    const payload: PostUpdateRequest = {
      title: title.value,
      content: content.value,
    };

    const updatedPost = await updatePost(props.postId, payload);

    alert('Post updated successfully!');
    await router.push(`/${updatedPost.id}`);

  } catch (err) {
    console.error('Failed to update post:', err);
    error.value = 'Failed to update post. Please try again.';
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div>
    <h2>Edit Post</h2>

    <div v-if="isLoading" class="loading">Loading...</div>
    <form v-else @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="title">Title</label>
        <input id="title" v-model="title" type="text" />
      </div>
      <div class="form-group">
        <label for="content">Content</label>
        <textarea id="content" v-model="content" rows="10"></textarea>
      </div>

      <p v-if="error" class="error-message">{{ error }}</p>

      <div class="form-actions">
        <button type="button" @click="router.push(`/${postId}`)" :disabled="isSubmitting">Cancel</button>
        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.loading {
  text-align: center;
  padding: 2rem;
}
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
  box-sizing: border-box;
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
