<script setup lang="ts">
import { computed, watch, onMounted, onActivated } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

function updateDataTheme() {
  const isDark = document.documentElement.classList.contains('dark');
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
}

onMounted(() => {
  document.documentElement.setAttribute('data-service', 'drive');
  updateDataTheme();

  if (isEmbedded.value) {
    import('portal/stores').then(({ useThemeStore }) => {
      const store = useThemeStore();

      if (store.isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
      updateDataTheme();

      watch(() => store.isDark, (newVal) => {
        if (newVal) {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
        updateDataTheme();
      });
    }).catch(() => {
      // Portal Shell themeStore load failed - ignore
    });
  } else {
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.attributeName === 'class') {
          updateDataTheme();
        }
      });
    });

    observer.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['class']
    });
  }
});

onActivated(() => {
  document.documentElement.setAttribute('data-service', 'drive');
  updateDataTheme();
});
</script>

<template>
  <div class="min-h-screen bg-bg-page">
    <!-- Header (Standalone only) -->
    <header
        v-if="!isEmbedded"
        class="bg-bg-card border-b border-border-default sticky top-0 z-50"
    >
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-primary to-brand-secondary flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">D</span>
            </div>
            <span class="text-xl font-bold text-text-heading">Drive</span>
          </router-link>

          <div class="px-3 py-1 bg-status-success-bg text-status-success text-sm font-medium rounded-full border border-status-success/20">
            Standalone
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />

        <div v-else class="max-w-5xl mx-auto px-6">
          <div class="bg-status-error-bg border border-status-error/20 rounded-lg p-8 text-center">
            <p class="text-xl text-status-error mb-4">
              Page not found: {{ route.path }}
            </p>
            <button
              class="px-4 py-2 bg-brand-primary text-white rounded-lg"
              @click="$router.push('/')"
            >
              Go Home
            </button>
          </div>
        </div>
      </router-view>
    </main>

    <!-- Footer (Standalone only) -->
    <footer
        v-if="!isEmbedded"
        class="bg-bg-card border-t border-border-default mt-auto"
    >
      <div class="max-w-7xl mx-auto px-4 py-6 text-center">
        <p class="text-sm text-text-meta">
          Portal Universe Drive
        </p>
      </div>
    </footer>
  </div>
</template>
