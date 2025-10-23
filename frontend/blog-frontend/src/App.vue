<script setup lang="ts">
import { computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Button } from '@portal/design-system';

const route = useRoute();
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

// 다크모드 감지 (Embedded 모드일 때만)
let themeStore: any = null;

onMounted(() => {
  if (isEmbedded.value) {
    try {
      // 동기 import 시도
      import('portal_shell/themeStore').then(({ useThemeStore }) => {
        themeStore = useThemeStore();

        // 초기 다크모드 적용
        if (themeStore.isDark) {
          document.documentElement.classList.add('dark');
        }

        // 다크모드 변경 감지
        watch(() => themeStore.isDark, (newVal) => {
          if (newVal) {
            document.documentElement.classList.add('dark');
          } else {
            document.documentElement.classList.remove('dark');
          }
        });
      }).catch((err) => {
        console.warn('Failed to load portal_shell themeStore:', err);
      });
    } catch (err) {
      console.warn('themeStore import failed:', err);
    }
  }
});
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header (Embedded 모드에서만 표시) -->
    <header v-if="!isEmbedded" class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-600 to-accent-600 flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">B</span>
            </div>
            <span class="text-xl font-bold text-gray-900 dark:text-gray-100">Blog</span>
          </router-link>

          <!-- Nav -->
          <nav class="flex items-center gap-6">
            <router-link
                to="/"
                class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              📄 Posts
            </router-link>
            <router-link
                to="/write"
                class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors"
                active-class="text-brand-600 font-bold"
            >
              ✍️ Write
            </router-link>
          </nav>

          <!-- Mode Badge (Standalone) -->
          <div class="px-3 py-1 bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-sm font-medium rounded-full">
            📦 Standalone
          </div>
        </div>
      </div>
    </header>

    <!-- Embedded Mode Badge (작은 표시) -->
    <div v-else class="bg-orange-50 dark:bg-orange-900/20 border-b border-orange-200 dark:border-orange-800">
      <div class="max-w-7xl mx-auto px-4 py-2">
        <p class="text-xs text-orange-600 dark:text-orange-400 font-medium">
          🔗 Embedded Mode (Portal Shell)
        </p>
      </div>
    </div>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <div v-else class="max-w-5xl mx-auto px-6">
          <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-8 text-center">
            <p class="text-xl text-yellow-800 dark:text-yellow-400 mb-4">❌ 페이지를 찾을 수 없습니다: {{ route.path }}</p>
            <Button variant="primary" @click="$router.push('/')">
              홈으로 돌아가기
            </Button>
          </div>
        </div>
      </router-view>
    </main>

    <!-- Footer (Standalone 모드에서만) -->
    <footer v-if="!isEmbedded" class="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 mt-auto">
      <div class="max-w-7xl mx-auto px-4 py-6 text-center">
        <p class="text-sm text-gray-600 dark:text-gray-400">© 2025 Portal Universe Blog. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>