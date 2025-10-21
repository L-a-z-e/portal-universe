<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

onMounted(() => {
  console.log('📍 [App.vue] Mounted');
  console.log('   Mode:', isEmbedded.value ? 'EMBEDDED' : 'STANDALONE');
  console.log('   Current route:', route.path);
  console.log('   Route name:', route.name);
});
</script>

<template>
  <div class="blog-app" :class="{ embedded: isEmbedded }">
    <header class="blog-header">
      <h1>📝 Blog Frontend</h1>
      <p class="mode-badge">
        {{ isEmbedded ? '🔗 Embedded Mode' : '📦 Standalone Mode' }}
      </p>
      <p class="route-info">Current: {{ $route.path }}</p>
    </header>

    <nav class="blog-nav">
      <router-link to="/">📄 Posts</router-link>
      <router-link to="/write">✍️ Write</router-link>
    </nav>

    <main class="blog-content">
      <router-view v-slot="{ Component }">
        <div v-if="Component">
          <component :is="Component" />
        </div>
        <div v-else class="no-match">
          <p>❌ No route matched: {{ $route.path }}</p>
          <router-link to="/">Go to home</router-link>
        </div>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.blog-app {
  border: 2px solid #42b983;
  padding: 1rem;
  border-radius: 8px;
}

.blog-app.embedded {
  border-color: #ff9800;
}

.blog-header {
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #ddd;
}

.blog-header h1 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
}

.mode-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.route-info {
  color: #666;
  font-size: 0.9rem;
  margin: 0.25rem 0 0 0;
}

.blog-nav {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.blog-nav a {
  padding: 0.5rem 1rem;
  background: #42b983;
  color: white;
  text-decoration: none;
  border-radius: 4px;
}

.blog-nav a:hover {
  background: #35a372;
}

.blog-nav a.router-link-active {
  background: #2c8960;
  font-weight: bold;
}

.blog-content {
  min-height: 200px;
}

.no-match {
  padding: 2rem;
  text-align: center;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 4px;
}
</style>