<script setup lang="ts">
  import { useAuthStore } from "portal_shell/authStore";
  import {onMounted, ref} from "vue";
  import {fetchAllPosts} from "../api/posts.ts";
  import type { PostResponse } from "../dto/PostResponse.ts";

  const authStore = useAuthStore();
  const posts = ref<PostResponse[]>([]);
  const isLoading = ref(true);
  const error = ref<string | null>(null);

  onMounted(async () => {
    try {
      isLoading.value = true;
      error.value = null;
      posts.value = await fetchAllPosts();
    } catch (err) {
      console.error('Failed to fetch posts:', err);
      error.value = 'Failed to fetch posts. Please try again later.';
    } finally {
      isLoading.value = false;
    }
  })
</script>
<template>
  <div>
    <h2>Blog Post List</h2>
    <hr/>
    <router-link v-if="authStore.isAuthenticated" to="/write">Write a new Post</router-link>
    <hr/>

    <!-- 로딩 중일 때 표시할 내용 -->
    <div v-if="isLoading">
      <p>Loading posts...</p>
    </div>

    <!-- 에러가 발생했을 때 표시할 내용 -->
    <div v-else-if="error">
      <p style="color: red;">{{ error }}</p>
    </div>

    <!-- 게시글이 있을 때 목록을 렌더링 -->
    <ul v-else-if="posts.length > 0">
      <li v-for="post in posts" :key="post.id">
        <!-- 각 게시글을 클릭하면 상세 페이지로 이동하는 링크 -->
        <router-link :to="`/${post.id}`">
          <h3>{{ post.title }}</h3>
        </router-link>
        <p>by {{ post.authorId }}</p>
      </li>
    </ul>

    <!-- 게시글이 하나도 없을 때 표시할 내용 -->
    <div v-else>
      <p>No posts found. Be the first to write one!</p>
    </div>
  </div>
</template>

<style scoped>
ul {
  list-style: none;
  padding: 0;
}
li {
  border-bottom: 1px solid #eee;
  padding: 1rem 0;
}
h3 {
  margin: 0;
}
</style>