<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { fetchPostById, deletePost } from '../api/posts';
import type { PostResponse } from '../dto/PostResponse';

const props = defineProps<{
  postId: string;
}>();

const router = useRouter();
const post = ref<PostResponse | null>(null);
const error = ref<string | null>(null);
const isLoading = ref(true);

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
  if (!post.value) return;

  const confirmed = confirm('Are you sure you want to delete this post?');
  if (confirmed) {
    try {
      await deletePost(post.value.id);
      alert('Post deleted successfully.');
      router.push('/');
    } catch (err) {
      console.error('Failed to delete post:', err);
      error.value = 'Failed to delete the post. Please try again.';
    }
  }
}
</script>

<template>
  <div class="post-detail-page">
    <div v-if="isLoading" class="loading">Loading post...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else-if="post" class="post-container">
      <h2 class="post-title">{{ post.title }}</h2>
      <p class="post-meta">
        <span>Author ID: {{ post.authorId }}</span> |
        <span>Created: {{ new Date(post.createdAt).toLocaleString() }}</span>
      </p>
      <div class="post-content">
        <p>{{ post.content }}</p>
      </div>
      <div class="post-actions">
        <router-link :to="`/`">Back to List</router-link>
        <div>
          <router-link :to="`/edit/${post.id}`" class="button">Edit</router-link>
          <button @click="handleDelete" class="button delete-button">Delete</button>
        </div>
      </div>
    </div>
    <div v-else class="not-found">
      <p>Post not found.</p>
      <router-link to="/">Go back to the list</router-link>
    </div>
  </div>
</template>

<style scoped>
.post-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}
.loading, .not-found {
  text-align: center;
  padding: 2rem;
}
.error-message {
  color: red;
  text-align: center;
}
.post-container {
  border: 1px solid #ccc;
  padding: 1.5rem;
  border-radius: 8px;
}
.post-title {
  margin-top: 0;
}
.post-meta {
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 1.5rem;
}
.post-content {
  white-space: pre-wrap; /* Preserve whitespace and newlines */
  line-height: 1.6;
}
.post-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.button {
  padding: 0.5rem 1rem;
  border: 1px solid #007bff;
  background-color: #007bff;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  margin-left: 0.5rem;
}
.button:hover {
  background-color: #0056b3;
}
.delete-button {
  background-color: #dc3545;
  border-color: #dc3545;
}
.delete-button:hover {
  background-color: #c82333;
}
</style>