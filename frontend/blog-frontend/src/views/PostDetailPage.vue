<script setup lang="ts">
  defineProps<{
    postId: string
  }>();
</script>
<template>
  <div>
    <h2>Post Detail Page</h2>
    <p>Showing content for Post ID: <strong>{{ postId }}</strong></p>
    <router-link to="/">Back to List</router-link>
  </div>
</template>
