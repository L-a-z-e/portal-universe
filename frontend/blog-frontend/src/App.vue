<script setup lang="ts">
/**
 * @file App.vue
 * @description Blog Frontend의 최상위 루트 컴포넌트입니다.
 * 실행 모드(Standalone/Embedded)에 따라 다른 레이아웃을 렌더링하고,
 * Embedded 모드일 경우 Portal Shell로부터 테마(다크 모드) 상태를 받아와 동기화합니다.
 */
import { computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { Button } from '@portal/design-system';

const route = useRoute();

// 현재 앱이 Portal Shell에 의해 임베드되었는지 여부를 확인합니다.
const isEmbedded = computed(() => window.__POWERED_BY_PORTAL_SHELL__ === true);

// Portal Shell의 테마 스토어 인스턴스
let themeStore: any = null;

onMounted(() => {
  // Embedded 모드일 때만 Portal Shell의 테마 스토어를 동적으로 가져옵니다.
  if (isEmbedded.value) {
    try {
      import('portal_shell/themeStore').then(({ useThemeStore }) => {
        themeStore = useThemeStore();

        // 초기 다크모드 상태를 <html> 태그에 적용합니다.
        if (themeStore.isDark) {
          document.documentElement.classList.add('dark');
        }

        // 셸의 테마 변경을 감지하여 이 앱의 다크모드를 동기화합니다.
        watch(() => themeStore.isDark, (newVal) => {
          document.documentElement.classList.toggle('dark', newVal);
        });
      }).catch((err) => {
        console.warn('Failed to load themeStore from portal_shell:', err);
      });
    } catch (err) {
      console.warn('Dynamic import of themeStore failed:', err);
    }
  }
});
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <!-- Header: Standalone 모드에서만 표시됩니다. -->
    <header v-if="!isEmbedded" class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 py-4">
        <div class="flex items-center justify-between">
          <router-link to="/" class="flex items-center gap-3 hover:opacity-80 transition-opacity">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-brand-600 to-accent-600 flex items-center justify-center shadow-lg">
              <span class="text-white font-bold text-lg">B</span>
            </div>
            <span class="text-xl font-bold text-gray-900 dark:text-gray-100">Blog</span>
          </router-link>

          <nav class="flex items-center gap-6">
            <router-link to="/" class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors" active-class="text-brand-600 font-bold">📄 Posts</router-link>
            <router-link to="/write" class="text-gray-600 dark:text-gray-300 hover:text-brand-600 font-medium transition-colors" active-class="text-brand-600 font-bold">✍️ Write</router-link>
          </nav>

          <div class="px-3 py-1 bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-sm font-medium rounded-full">
            📦 Standalone Mode
          </div>
        </div>
      </div>
    </header>

    <!-- Embedded Mode Badge: 셸에 포함되었을 때 작은 배지를 표시합니다. -->
    <div v-else class="bg-orange-50 dark:bg-orange-900/20 border-b border-orange-200 dark:border-orange-800">
      <div class="max-w-7xl mx-auto px-4 py-2">
        <p class="text-xs text-orange-600 dark:text-orange-400 font-medium">
          🔗 Embedded Mode (Loaded by Portal Shell)
        </p>
      </div>
    </div>

    <!-- Main Content -->
    <main :class="isEmbedded ? 'py-4' : 'py-8'">
      <router-view v-slot="{ Component }">
        <component :is="Component" v-if="Component" />
        <!-- 라우팅 경로에 해당하는 컴포넌트가 없을 경우의 Fallback UI -->
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

    <!-- Footer: Standalone 모드에서만 표시됩니다. -->
    <footer v-if="!isEmbedded" class="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 mt-auto">
      <div class="max-w-7xl mx-auto px-4 py-6 text-center">
        <p class="text-sm text-gray-600 dark:text-gray-400">© 2025 Portal Universe Blog. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>
