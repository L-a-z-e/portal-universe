<script setup lang="ts">
  import { defineAsyncComponent} from "vue";
  import BlogApp from 'blog_remote/BlogApp';
  // const BlogApp = defineAsyncComponent(() => import('blog_remote/BlogApp'));
</script>

<template>
  <div style="border: 2px solid green; padding: 1rem">
    <h1>This is Portal Shell (Host Application)</h1>
    <p>Serving from localhost:5000</p>
    <hr/>
    <h2>Dynamically Loaded Micro Frontend</h2>
    <Suspense>
      <BlogApp />
      <template #fallback>
        <div>Loading Micro Frontend...</div>
      </template>
    </Suspense>
  </div>
</template>